package re;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
//import java.util.Arrays;
import java.util.regex.*;
import java.util.regex.Pattern;

public class RE {
	public static String strSep = "*";
	public static String strEnter = "\n";
	
	public static String[] hasTail = {"MessageID", "MessageTimestamp", "EventTimestamp", "TxnTimestamp", "PatientUID", "StartTimestamp", 
			"AdditionalInfo", "houseNumber", "additionalLocator", "unitID",	"TypeCode", "TypeDesc", "name", "BirthDateTime", "GenderCode", 
			"GenderDesc", "RaceCode", "RaceDesc", "ReligionCode", "ReligionDesc", "ExtensionNumber", "TelephoneTypeCode","AdmitDateTime", 
			"DischargeDateTime", "CaseTypeCode", "LicenseNumber", "Name", "CareProviderRoleCode", "CareProviderRoleDescription", 
			"FindingDateTime", "FindingStatusCode", "FindingStatusDescription", "PriorityDescription", "PriorityCode", "ProcedureCatalogCode", 
			"ProcedureCatalogDescription", "ProcedureServiceInformation", "ChangedOn", 
			"CreatedOn", "LabTypeCode", "LabTypeCodeDescription", "LabGroupCode", "ChapterCode", "SpecimenCollectionDateTime", 
			"SpecimenReceivedDateTime", "VerificationDateTime", "TypeOfOrder"};
	
	public static String[] noTail = {"IDNumber", "PatientIdentificationNumber", "CaseIdentificationNumber", "OrderIdentificationNumber"};
	//ProcedureServiceInformation
	public static String[] procServInfo = {"ProcedureCode", "ProcedureCodeDescription"};
	//ServiceOrderObservation, NEW TABLE
	public static String[] servOrderObserv = {"ProcedureCode", "ProcedureCodeDescription", "ProcedureExtentDescription", "FindingCode",
			"FindingDescription", "FindingStatusCode", "FindingStatusDescription", "FindingAmount", "FindingAmountDescription", 	"FindingAmountUnitCode", 
			"FindingComments", "ValueType"};
	
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
		String strLTFile = "LabTest.txt";
		String strObservFile = "Observation.txt";
		String strLogFile = "Log.txt";
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		
		FileWriter fwLT = new FileWriter(strdir + strLTFile);
		FileWriter fwLog = new FileWriter(strdir + strLogFile);
		FileWriter fwObserv = new FileWriter(strdir + strObservFile);
		
		PrintSchema(fwLT, fwObserv);
		
		int i = 1;
		for (File f: files)
		{
			String s = f.getPath();
			strXML = ReadFile(s);
			Parse(strXML, fwLT, fwObserv);
			WriteFile(fwLog, s + " has been processed successfully. Row " + Integer.toString(i) + " in the LT file.\n");
			fwLog.flush();
			//System.out.println(s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.");
			i++;
		}	
		endTime=System.currentTimeMillis();
		WriteFile(fwLog, "time cost " + (endTime - startTime) + "ms.\n");
		//System.out.println("time cost " + (endTime - startTime) + "ms");
		
		fwLT.close();
		fwObserv.close();
		fwLog.close();
	}

	public void PrintSchema(FileWriter fwLT, FileWriter fwObserv) throws Exception
	{
		String SchemaLT = "";
		String SchemaObservation = "CaseIdentificationNumber*AdmitDateTime*DischargeDateTime*FindingDateTime*";
		for (String str: noTail)
		{
			SchemaLT += (str + strSep);
		}
		for (String str: hasTail)
		{
			if (str == "ProcedureServiceInformation")
			{
				SchemaLT += (str + " " + procServInfo[0] +  strSep);
				SchemaLT += (str + " " + procServInfo[1] +  strSep);
			}
			else
			{
				if (str == hasTail[hasTail.length - 1])
					SchemaLT += (str + strEnter);
				else
					SchemaLT += (str + strSep);
			}
		}
		for (String str: servOrderObserv)
		{
			if (str == servOrderObserv[servOrderObserv.length - 1])
				SchemaObservation += (str + strEnter);
			else
				SchemaObservation += (str + strSep);
		}
		
		WriteFile(fwLT, SchemaLT);
		WriteFile(fwObserv, SchemaObservation);
	}
	public void Parse(String strXML, FileWriter fwLT, FileWriter fwObserv) throws Exception
	{
		String strParse = "";
		String strCaseID = "";
		String strAdmitDateTime = "";
		String strDischargeDateTime = "";
		String strFindingDateTime = "";
		
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
					if (str == "AdmitDateTime")
						strAdmitDateTime = m.group(1);
					if (str == "DischargeDateTime")
						strDischargeDateTime = m.group(1);
					if (str == "FindingDateTime")
						strFindingDateTime = m.group(1);
				}
				// for the last one, add a '\n';
				if (str != hasTail[hasTail.length - 1])
					strParse += strSep;
				else
					strParse += strEnter;
			}
		}
		strAdmitDateTime += strSep;
		strDischargeDateTime += strSep;
		strFindingDateTime += strSep;
		
		//case 3: observation table, new file to store
		String strObserv = "ServiceOrderObservation";
		String strParseObserv = "";
		Pattern p = Pattern.compile("<[^<A-Z]*" + strObserv + "[^>]*>(((?!" + strObserv + ").)*)</[^>A-Z]*" + strObserv + ">", Pattern.DOTALL);
		Matcher m = p.matcher(strXML);
		while (m.find())
		{
			
			String strNextLevelXML = m.group(1);
			strParseObserv += strCaseID + strAdmitDateTime + strDischargeDateTime + strFindingDateTime;
			strParseObserv += ParseNextLevel(strNextLevelXML, strObserv);
		}
		WriteFile(fwLT, strParse);
		WriteFile(fwObserv, strParseObserv);
	}
	
	public String ParseNextLevel (String strXML, String tag)
	{
		String strParseNL = "";
		
		if (tag == "ProcedureServiceInformation")
		{
			for (String str : procServInfo)
			{
				Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
				Matcher m = p.matcher(strXML);
			
				if (m.find())
				{
					strParseNL += m.group(1);
				}
				strParseNL += strSep;
			}
		}
		else 
		{
			for (String str : servOrderObserv)
			{
				Pattern p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
				Matcher m = p.matcher(strXML);
			
				if (m.find())
				{
					strParseNL += m.group(1);
				}
				if (str == servOrderObserv[servOrderObserv.length - 1])
					strParseNL += strEnter;
				else 
					strParseNL += strSep;
			}
		}
		return strParseNL;
	}
	
	public static void main(String[] args) throws Exception
    {
		RE t = new RE();
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		String strdir = "/home/xr/workspace/20151128/ltdata/";
		t.ParseDir(strdir);
		endTime=System.currentTimeMillis();
		System.out.println("time cost " + (endTime - startTime) + "ms");
    }
}
