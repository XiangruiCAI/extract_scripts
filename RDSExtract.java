// extract ORM table

package re;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.regex.*;

public class ORMExtract {
	public static String strSep = "*";
	public static String strEnter = "\n";
	
	public static String[] hasTail = {"MessageID", "MessageTimestamp", "EventType", "TxnTimestamp", "PatientUID",
			"StartTimestamp", "QueueName", "AdditionalInfo", "SemanticGroup", "postalCode", "streetAddressLine",
			"houseNumber", "additionalLocator", "unitID", "TypeCode", "TypeDesc",
			"name", "BirthDateTime", "ResidentIndicator", "DeathIndicator", "GenderCode", "GenderDesc", "MaritalStatusCode",
			"RaceCode", "ExtensionNumber", "TelephoneTypeCode", "AdmitDateTime", "CaseTypeCode",
			"CaseTypeDescription", "PatientClassCode", "PatientClassDescription", "SpecialtyCode", "SpecialtyDescription"};
	
	public static String[] noTail = {"IDNumber", "PatientIdentificationNumber", "CaseIdentificationNumber"};

	//CareProviderRole
	public static String[] careProviderRole = {"LicenseNumber", "CareProviderRoleCode", "CareProviderRoleDescription"};
	
	//ServiceOrder (OrderIdentificationNumber is a notail case)
	//CareProviderInvolvement and MedicationDispense will be parsed separately
	public static String[] serviceOrder = {"OrderingLocationCode", "OrderingLocationDescription", "OrderDateTime", "OrderStatusCode", 
			"OrderStatusDescription", "DosageFrequencyCode", "DosageFrequencyDescription", "DispenseDosageAmountUnitCode", 
			"DispenseQuantityAmount", "DispenseQuantityUnitCode", "DispenseQuantityUnitDescription", "DispenseStrengthAmount", 
			"DispenseStrengthUnitCode", "DispenseDateTime", "DispenseInstructionDescription", "DrugCode", "DrugName", "DrugFormCode", 
			"DrugFormDescription", "DispenseTypeCode", "DurationCode", "DurationDescription", "PharmacistInterventionIndicator", 
			"PrescriptionIdentificationNumber", "PrescriptionSequenceNumber", "ChangedOn"};
	//CareProviderInvolvement
	public static String[] careProviderInvolvement = {"LicenseNumber", "Name", "CareProviderRoleCode", "CareProviderRoleDescription"};
	
	private String strOrderID = "";
	private String strPrescriptionID = "";
	private String strPrescriptionNo = "";
	
  	public String ReadFile(String strIn) throws Exception
	{
		File file = new File(strIn);

		StringBuilder sb = new StringBuilder();
		String s ="";
		BufferedReader br = new BufferedReader(new FileReader(file));

		while( (s = br.readLine()) != null) {
			sb.append(s.trim() + "|||");
		}

		br.close();
		return sb.toString();
	}
	
	public void WriteFile(FileWriter fw, String strContent) throws Exception
	{
		try
		{
				fw.append(strContent); 
		}
		catch (Exception e)
		{
            e.printStackTrace();
        }	
	}
	
	public void ParseDir(String strdir) throws Exception
	{
		File dir = new File(strdir);
		File[] files = dir.listFiles();
		//Arrays.sort(files);
		String strXML = "";
		String[] strOutput = {"DMed.txt", "CareProviderRole.txt", "ServiceOrder.txt", "ServOrdCPI.txt", "MedDispCPI.txt", "Log.txt"};
		FileWriter[] fw = new FileWriter[6];
		int j;
		for (j = 0; j < 6; j++)
			fw[j] = new FileWriter(strdir + strOutput[j]);

		long startTime, endTime;
		startTime=System.currentTimeMillis();
		
		PrintSchema(fw);
		int i = 1;
		for (File f: files)
		{
			String s = f.getPath();
			strXML = ReadFile(s);
			Parse(strXML, fw);
			WriteFile(fw[5], s + " processed . Row " + Integer.toString(i) + ".\n");
			fw[5].flush();
			//System.out.println(s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.");
			i++;
		}	
		endTime=System.currentTimeMillis();
		WriteFile(fw[5], "time cost " + (endTime - startTime) + "ms.\n");
		//System.out.println("time cost " + (endTime - startTime) + "ms");
		
		for (j = 0; j < 6; j++)
		{
			fw[j].close();
		}
	}

	public void PrintSchema(FileWriter[] fw) throws Exception
	{
		String SchemaDispensedMed = "";
		String SchemaMedCareProviderRole = "CaseIdentificationNumber*LicenseNumber*CareProviderRoleCode*CareProviderRoleDescription\n";
		String SchemaServiceOrder = "CaseIdentificationNumber*OrderIdentificationNumber*";
		// two different CareProviderInvolvement
		String SchemaServOrdCPI = "CaseIdentificationNumber*OrderIdentificationNumber*";
		String SchemaMedDispCPI = "CaseIdentificationNumber*OrderIdentificationNumber*PrescriptionIdentificationNumber*PrescriptionSequenceNumber*";
		for (String str: noTail)
		{
			SchemaDispensedMed += (str + strSep);
		}
		for (String str: hasTail)
		{
			if (str == hasTail[hasTail.length - 1])
				SchemaDispensedMed += (str + strEnter);
			else
				SchemaDispensedMed += (str + strSep);
		}
		for (String str: serviceOrder)
		{
			if (str == serviceOrder[serviceOrder.length - 1])
				SchemaServiceOrder += (str + strEnter);
			else 
				SchemaServiceOrder += (str + strSep);
		}
		for (String str: careProviderInvolvement)
		{
			if (str == careProviderInvolvement[careProviderInvolvement.length - 1])
			{
				SchemaMedDispCPI += (str + strEnter);
				SchemaServOrdCPI += (str+strEnter);
			}
			else
			{
				SchemaMedDispCPI += (str + strSep);
				SchemaServOrdCPI += (str+strSep);
			}
		}
		
		WriteFile(fw[0], SchemaDispensedMed);
		WriteFile(fw[1], SchemaMedCareProviderRole);
		WriteFile(fw[2], SchemaServiceOrder);
		WriteFile(fw[3], SchemaServOrdCPI);
		WriteFile(fw[4], SchemaMedDispCPI);
	}
	public void Parse(String strXML, FileWriter[] fw) throws Exception
	{
		String strParse = "";
		String strCaseID = "";
		
		// case 1: patterns with no tails
		for (String str : noTail)
		{
			Pattern p = Pattern.compile("<.*" + str + " extension=\"([^\"]*)\"/>", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				strParse += m.group(1);
				if (str == "CaseIdentificationNumber")
					strCaseID = m.group(1) + strSep;
			}
			strParse += strSep;
		}
				
		// case 2: patterns with tails
		for (String str : hasTail)
		{
			//Pattern p = Pattern.compile("<[^A-Z]*" + str + ".*>(((?!" + str + ").)*)</[^A-Z]*" + str + ">", Pattern.DOTALL);
			Pattern p = Pattern.compile("<[^<A-Z]*" + str + "[^>]*>(((?!" + str + ").)*)</[^>A-Z]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				strParse += m.group(1);			
			}
			// for the last one, add a '\n';
			if (str != hasTail[hasTail.length - 1])
				strParse += strSep;
			else
				strParse += strEnter;
		}	//DMed parse end;
		
		//CareProviderRole table, new file to store
		String tagCPR = "CareProviderRole";
		String strCPR = "";
		Pattern p = Pattern.compile("<[^<]*" + tagCPR + ">(.*?)<[^<]*" + tagCPR + ">", Pattern.DOTALL);
		Matcher m = p.matcher(strXML);
		while (m.find())
		{
			String strNextLevelXML = m.group(1);
			strCPR += strCaseID;
			strCPR += ParseCPR(strNextLevelXML);
		} // CareProviderRole parse end;
		
		// ServiceOrder table
		String tagSO = "ServiceOrder";
		String strSO = "";
		String tagSOCPI = "CareProviderInvolvement";
		String strSOCPI = "";
		String tagMDCPI = "CareProviderInvolvement";
		String strMDCPI = "";
		
		Pattern p2 = Pattern.compile("<[^<]*" + tagSO + ">(.*?)<[^<]*" + tagSO + ">", Pattern.DOTALL);
		Matcher m2 = p2.matcher(strXML);
		while (m2.find())
		{
			String strNextLevelXML = m2.group(1);
			// ServiceOrder parse;
			strSO += strCaseID;
			strSO += ParseSO(strNextLevelXML);
			
			// ServiceOrder CareProviderInvolvement parse;
			String strRegex = "<[^<]*" + "MedicationDispense" + ">.*<[^<]*" +"MedicationDispense" + ">";
			String strServOrdCPI = strNextLevelXML.replaceAll(strRegex, "");
			Pattern p3 = Pattern.compile("<[^<]*" + tagSOCPI + ">(.*?)<[^<]*" + tagSOCPI + ">", Pattern.DOTALL);
			Matcher m3 = p3.matcher(strServOrdCPI);
			while (m3.find())
			{
				strSOCPI += strCaseID;
				strSOCPI += ParseSOCPI(strServOrdCPI);
			}
			
			// MedicationDispense CareProviderInvolvement parse;
			Pattern p4 = Pattern.compile("<[^<]*" + "MedicationDispense" + ">(.*?)<[^<]*" + "MedicationDispense" + ">", Pattern.DOTALL);
			Matcher m4 = p4.matcher(strNextLevelXML);
			if (m4.find())
			{
				String strMedDispCPI = m4.group(1);
				Pattern p5 = Pattern.compile("<[^<]*" + tagMDCPI + ">(.*?)<[^<]*" + tagMDCPI + ">", Pattern.DOTALL);
				Matcher m5 = p5.matcher(strMedDispCPI);
				while (m5.find())
				{
					strMDCPI += strCaseID + strOrderID + strSep + strPrescriptionID + strSep + strPrescriptionNo + strSep;
					strMDCPI += ParseMDCPI(m5.group(1));
				}
			}
		} 
		
		WriteFile(fw[0], strParse);
		WriteFile(fw[1], strCPR);
		WriteFile(fw[2], strSO);
		WriteFile(fw[3], strSOCPI);
		WriteFile(fw[4], strMDCPI);
	}
	
	public String ParseCPR (String strXML)
	{
		String strParseNL = "";
		for (String str : careProviderRole)
		{
			Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			if (str == careProviderRole[careProviderRole.length - 1])
				strParseNL += strEnter;
			else
				strParseNL += strSep;
		}
		
		return strParseNL;
	}

	public String ParseSO (String strXML)
	{
		String strParseNL = "";
		Pattern p0 = Pattern.compile("<.*" + "OrderIdentificationNumber" + " extension=\"([^\"]*)\"/>", Pattern.DOTALL);
		Matcher m0 = p0.matcher(strXML);
		if (m0.find())
		{
			strParseNL += m0.group(1);
			strOrderID = m0.group(1);
		}
		strParseNL += strSep;
		
		for (String str : serviceOrder)
		{
			Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
				if (str == "PrescriptionIdentificationNumber")
					strPrescriptionID = m.group(1);
				if (str == "PrescriptionSequenceNumber")
					strPrescriptionNo = m.group(1);
			}
			
			if (str == serviceOrder[serviceOrder.length - 1])
				strParseNL += strEnter;
			else
				strParseNL += strSep;
		}
		//strOrderID += strSep;
		//strPrescriptionID += strSep;
		//strPrescriptionNo += strSep;
		
		return strParseNL;
	}
	
	public String ParseSOCPI (String strXML)
	{
		String strParseNL = "";
		Pattern p0 = Pattern.compile("<.*" + "OrderIdentificationNumber" + " extension=\"([^\"]*)\"/>", Pattern.DOTALL);
		Matcher m0 = p0.matcher(strXML);
		if (m0.find())
		{
			strParseNL += m0.group(1);
			//strOrderID = m0.group(1);
		}
		strParseNL += strSep;
		
		for (String str : careProviderInvolvement)
		{
			Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			if (str == careProviderInvolvement[careProviderInvolvement.length - 1])
				strParseNL += strEnter;
			else
				strParseNL += strSep;
		}
		
		return strParseNL;
	}	
	
	public String ParseMDCPI (String strXML)
	{
		String strParseNL = "";
		
		for (String str : careProviderInvolvement)
		{
			Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			if (str == careProviderInvolvement[careProviderInvolvement.length - 1])
				strParseNL += strEnter;
			else
				strParseNL += strSep;
		}
		
		return strParseNL;
	}	

	public static void main(String[] args) throws Exception
    {
		ORMExtract t = new ORMExtract();
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		String strdir = "/home/xr/data/export1115/RDS_MSG/";
		t.ParseDir(strdir);
		endTime=System.currentTimeMillis();
		System.out.println("time cost " + (endTime - startTime) + "ms");
    }
}