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
	
	public static String[] hasTail = {"MessageID", "MessageTimestamp", "EventType", "TxnTimestamp", "PatientUID", "StartTimestamp", "QueueName", 
			"AdditionalInfo", "SemanticGroup", "name", "BirthDateTime", "GenderCode", "GenderDesc",
			"CaseTypeCode", "CaseTypeDescription", "PatientClassCode", "PatientClassDescription", "SpecialtyCode",
			"SpecialtyDescription", "LicenseNumber", "Name", "CareProviderRoleCode", "CareProviderRoleDescription", 
			"OrderDateTime", "TransactionDateTime", "OrderDescription", "OrderStatusCode", "OrderStatusDescription"};
	
	public static String[] noTail = {"IDNumber", "PatientIdentificationNumber", "CaseIdentificationNumber", "OrderIdentificationNumber"};
	//MedicationOrder
	public static String[] medicationOrder = {"DrugCode", "DrugName", "DrugStrengthAmount", "DrugStrengthDescription", 
			"DrugFormCode", "DrugFormDescription", "ExternalDrugIndicator", "OrderSequenceNumber", "ChangedOn", 
			"OrderInstructionDescription", "PackagingCode", "OrderQuantityAmount", "OrderQuantityUnitCode", 
			"OrderQuantityUnitDescription", "OrderTypeCode", "OrderFrequencyCode", "OrderFrequencyDescription",
			"OrderDurationCode", "OrderDurationCodeDescription", "DispenseDosageAmount"};
	
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
		String strLTFile = "PrescribedMedication.txt";
		String strObservFile = "MedicationOrder.txt";
		String strLogFile = "Log.txt";
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		
		FileWriter fwPMed = new FileWriter(strdir + strLTFile);
		FileWriter fwLog = new FileWriter(strdir + strLogFile);
		FileWriter fwMedOrder = new FileWriter(strdir + strObservFile);
		
		PrintSchema(fwPMed, fwMedOrder);
		
		int i = 1;
		for (File f: files)
		{
			String s = f.getPath();
			strXML = ReadFile(s);
			Parse(strXML, fwPMed, fwMedOrder);
			WriteFile(fwLog, s + " processed . Row " + Integer.toString(i) + ".\n");
			fwLog.flush();
			//System.out.println(s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.");
			i++;
		}	
		endTime=System.currentTimeMillis();
		WriteFile(fwLog, "time cost " + (endTime - startTime) + "ms.\n");
		//System.out.println("time cost " + (endTime - startTime) + "ms");
		
		fwPMed.close();
		fwMedOrder.close();
		fwLog.close();
	}

	public void PrintSchema(FileWriter fwPMed, FileWriter fwMedOrder) throws Exception
	{
		String SchemaPrescribedMed = "";
		String SchemaMedOrder = "CaseIdentificationNumber*OrderDateTime*TransactionDateTime*";
		for (String str: noTail)
		{
			SchemaPrescribedMed += (str + strSep);
		}
		for (String str: hasTail)
		{
			if (str == hasTail[hasTail.length - 1])
				SchemaPrescribedMed += (str + strEnter);
			else
				SchemaPrescribedMed += (str + strSep);
		}
		for (String str: medicationOrder)
		{
				SchemaMedOrder += (str + strSep);
		}
		
		WriteFile(fwPMed, SchemaPrescribedMed);
		WriteFile(fwMedOrder, SchemaMedOrder + "FindingCode" + strEnter);
	}
	public void Parse(String strXML, FileWriter fwLT, FileWriter fwObserv) throws Exception
	{
		String strParse = "";
		String strCaseID = "";
		String strOrderDateTime = "";
		String strTransactionDateTime = "";
		
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
			if (str == "ProcedureServiceInformation")
			{
				if (m.find())
				{
					String strNextLevelXML = m.group(1);
					String tmp = ParseNextLevel(strNextLevelXML, str);
					strParse += tmp;					
				}
				// if not find, still need to add separator
				else
				{
					strParse += (strSep + strSep);
				}
			}
			else
			{
				if (m.find())
				{
					strParse += m.group(1);			
					if (str == "OrderDateTime")
						strOrderDateTime = m.group(1);
					if (str == "TransactionDateTime")
						strTransactionDateTime = m.group(1);
				}
				// for the last one, add a '\n';
				if (str != hasTail[hasTail.length - 1])
					strParse += strSep;
				else
					strParse += strEnter;
			}
		}
		strOrderDateTime += strSep;
		strTransactionDateTime += strSep;
		
		//case 3: MedicationOrder table, new file to store
		String strMedOrder = "MedicationOrder";
		String strMedicationOrder = "";
		Pattern p = Pattern.compile("<[^<]*" + strMedOrder + ">(.*?)<[^<]*" + strMedOrder + ">", Pattern.DOTALL);
		Matcher m = p.matcher(strXML);
		while (m.find())
		{
			String strNextLevelXML = m.group(1);
			strMedicationOrder += strCaseID + strOrderDateTime + strTransactionDateTime;
			strMedicationOrder += ParseNextLevel(strNextLevelXML, strMedOrder);
		}
		WriteFile(fwLT, strParse);
		WriteFile(fwObserv, strMedicationOrder);
	}
	
	public String ParseNextLevel (String strXML, String tag)
	{
		String strParseNL = "";
		for (String str : medicationOrder)
		{
			Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			strParseNL += strSep;
		}
		// save all FindingCode together, assuming they are close to each other.
		String strFC = "FindingCode";
		Pattern p = Pattern.compile("<[^<]*" + strFC + ">(.*)</" + strFC + ">", Pattern.DOTALL);
		Matcher m = p.matcher(strXML);
		
		if (m.find())
		{
			strParseNL += m.group(1);
		}
		strParseNL += strEnter;
		
		return strParseNL;
	}
	
	public static void main(String[] args) throws Exception
    {
		ORMExtract t = new ORMExtract();
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		String strdir = "/home/xr/workspace/ORM_Extract/Medication/ORM_MSG/";
		t.ParseDir(strdir);
		endTime=System.currentTimeMillis();
		System.out.println("time cost " + (endTime - startTime) + "ms");
    }
}