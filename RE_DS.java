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
	
	public static String[] hasTail = {"MessageID", "MessageTimestamp", "EventTimestamp", "TxnTimestamp", 
			"PatientUID", "StartTimestamp", "AdditionalInfo", "country", "countrydescription", "postalCode", "streetAddressLine",
			"houseNumber", "streetName", "additionalLocator", "unitID", "PatientClassCode", "PatientClassDescription", 
			"name", "BirthDateTime", "GenderCode", "GenderDesc", "RaceCode", "RaceDesc", "VIPGroup", "AdmissionTypeCode", 
			"AdmissionTypeDescription", "AdmitDateTime", "CaseTypeCode", "CaseTypeDescription", "ActualDischargeDateTime", 
			"HistoryAndPhysicalFindings", "OutcomeFollowupPlan", "OutcomeOnDischarge",	"RelevantTreatmentAndInvestigations",
			"Complications", "PlanCode", "PlanDescription", "PrincipalProcedure", "ComplicationsAndComorbidities",
			"DischargeOutcome", "CompletedDate", "EHIDSStatus", "MedicalService", "CreateBy", "LeaveType", "UnFitFrom", 
			"UnFitTo", "Status"};
	public static String[] noTail = {"IDNumber", "PatientIdentificationNumber", "CaseIdentificationNumber", "CareProviderIdentificationNumber"};
	public static String[] newTable = {"DiagnosisList", "CareProviderInvolvement", "MedicationDetails"};
	//"DiagnosisList"
	public static String[] diagnosis = {"DiagnosisStatusDescription", "DiagnosisCatalogCode", "DiagnosisCatalogDescription",
			"DiagnosisCode", "DiagnosisDescription", "DRGDiagnosisCode", "DRGDiagnosisCodeDescription", "DiagnosisTypeCode", 
			"DiagnosisTypeDescription", "DiagnosisComments"};
	// CareProviderInvolvement, PerformedBy, CreateBy(unique, different from CreatedBy)
	public static String[] careProviderInvolvement = {"LicenseNumber", "Name", "CareProviderRoleCode", "CareProviderRoleDescription"};
	//MedicationDetails
	public static String[] medications = {"SequenceNo", "MedicationName", "DosageRegimen"};
	
	// "OutcomeOnDischarge", "ComplicationsAndComorbidities", "DischargeOutcome", "MedicalService", "LeaveBy", "EHIDSStatus"
	public static String[] sharedChild = {"Code", "Description"};
	// "PrincipalProcedure"
	public static String[] principalProcedure = {"Code", "Description", "PerformedBy", "PerformedDate"};
	
	public String ReadFile(String strIn) throws Exception
	{
		File file = new File(strIn);

		StringBuilder sb = new StringBuilder();
		String s ="";
		BufferedReader br = new BufferedReader(new FileReader(file));

		while( (s = br.readLine()) != null) {
			sb.append(s + "|||");
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
		String strDSFile = "DS.txt";
		String strDiagFile = "Diag.txt";
		String strCPFile = "CP.txt";
		String strMedFile = "Med.txt";
		String strLogFile = "Log.txt";
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		
		FileWriter fwDS = new FileWriter(strdir + strDSFile);
		FileWriter fwDiag = new FileWriter(strdir + strDiagFile);
		FileWriter fwCP = new FileWriter(strdir + strCPFile);
		FileWriter fwMed = new FileWriter(strdir + strMedFile);
		FileWriter fwLog = new FileWriter(strdir + strLogFile);
		
		PrintSchema(fwDS, fwDiag, fwCP, fwMed);
		
		int i = 1;
		for (File f: files)
		{
			String s = f.getPath();
			strXML = ReadFile(s);
			Parse(strXML, fwDS, fwDiag, fwCP, fwMed);
			WriteFile(fwLog, s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.\n");
			fwLog.flush();
			//System.out.println(s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.");
			i++;
		}	
		endTime=System.currentTimeMillis();
		WriteFile(fwLog, "time cost " + (endTime - startTime) + "ms.\n");
		//System.out.println("time cost " + (endTime - startTime) + "ms");
		
		fwDS.close();
		fwDiag.close();
		fwCP.close();		
		fwMed.close();	
		fwLog.close();
	}

	public void PrintSchema(FileWriter fwDS, FileWriter fwDiag, FileWriter fwCP, FileWriter fwMed) throws Exception
	{
		String SchemaDS = "";
		String SchemaDiag = "CaseIdentificationNumber*AdmitDateTime*ActualDischargeDateTime*EHIDSStatus Code*EHIDSStatus Description*CompletedDate*";
		String SchemaCP = "CaseIdentificationNumber*AdmitDateTime*ActualDischargeDateTime*EHIDSStatus Code*EHIDSStatus Description*CompletedDate*";
		String SchemaMed = "CaseIdentificationNumber*AdmitDateTime*ActualDischargeDateTime*EHIDSStatus Code*EHIDSStatus Description*CompletedDate*";
		
		for (String str: noTail)
		{
			SchemaDS += (str + strSep);
		}
		for (String str: hasTail)
		{
			if (str == "PrincipalProcedure")
			{
				for (String str2 : principalProcedure)
				{
					SchemaDS += (str + " " + str2 + strSep);
				}
			}
			else if (str == "CreateBy")
			{
				for (String str2 : careProviderInvolvement)
				{
						SchemaDS += (str + " " + str2 + strSep);
				}
			}
			else if ( str == "OutcomeOnDischarge" || str =="ComplicationsAndComorbidities"  
					|| str == "DischargeOutcome" || str == "EHIDSStatus" 
					|| str == "MedicalService" || str == "LeaveType")
			{
				SchemaDS += (str + " " + sharedChild[0] + strSep);
				SchemaDS += (str + " " + sharedChild[1] + strSep);
			}
			else
			{
				if (str == "Status")
					SchemaDS += (str + strEnter);
				else
					SchemaDS += (str + strSep);
			}
		}
		
		for (String str : newTable)
		{
			if (str == "DiagnosisList")
			{
				for (String str2 : diagnosis)
				{
					if (str2 == diagnosis[diagnosis.length - 1])
						SchemaDiag += (str2 + strEnter);
					else
						SchemaDiag += (str2 + strSep);
				}
			}
			else if (str == "CareProviderInvolvement")
			{
				for (String str2 : careProviderInvolvement)
				{
					if (str2 == careProviderInvolvement[careProviderInvolvement.length - 1])
						SchemaCP += (str2 + strEnter);
					else
						SchemaCP += (str2 + strSep);
				}
			}
			else if (str == "MedicationDetails")
			{
				for (String str2 : medications)
				{
					if (str2 == medications[medications.length - 1])
						SchemaMed += (str2 + strEnter);
					else
						SchemaMed += (str2 + strSep);
				}
			}
		}
		WriteFile(fwDS, SchemaDS);
		WriteFile(fwDiag, SchemaDiag);
		WriteFile(fwCP, SchemaCP);
		WriteFile(fwMed, SchemaMed);
	}
	public void Parse(String strXML, FileWriter fwDS, FileWriter fwDiag, FileWriter fwCP, FileWriter fwMed) throws Exception
	{
		String strParse = "";
		String strParseDiag = "";
		String strParseCP = "";
		String strParseMed = "";
		String strCaseID = "";
		String strAdmitDateTime = "";
		String strActualDischargeDateTime = "";
		String strEHIDSStatus = "";						// Code + '*' + Description
		String strCompletedDate = "";
		
		// case 1: patterns with no tails
		for (String str : noTail)
		{
			Pattern p = Pattern.compile("<.*" + str + " extension=\"([^\"]*)\"/>", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				if (str == "CaseIdentificationNumber")
					strCaseID = m.group(1) + strSep;
				strParse += m.group(1);
			}
			strParse += strSep;
		}
				
		// case 2: patterns with tails
		for (String str : hasTail)
		{
			Pattern p = Pattern.compile("<[^<A-Z]*" + str + ">(((?!" + str + ").)*)</[^>A-Z]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (str == "PrincipalProcedure" || str == "CreateBy" || str == "OutcomeOnDischarge" 
					|| str =="ComplicationsAndComorbidities"  || str == "DischargeOutcome" 
					|| str == "EHIDSStatus" || str == "MedicalService" || str == "LeaveType")
			{
				if (m.find())
				{
					String strNextLevelXML = m.group(1);
					String tmp = ParseNextLevel(strNextLevelXML, str);
					strParse += tmp;
					
					if (str == "EHIDSStatus")
						strEHIDSStatus = tmp;						
				}
				// å¦‚æžœç¬¬ä¸€å±‚æ²¡æœ‰ï¼Œè¦�åŠ ä¸Šå¯¹åº”ä¸ªæ•°çš„separator
				else
				{
					strParse += (strSep + strSep);
					if (str == "PrincipalProcedure" || str == "CreateBy")
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
					if (str == "ActualDischargeDateTime")
						strActualDischargeDateTime = m.group(1);
					if (str == "CompletedDate")
						strCompletedDate = m.group(1);					
				}
				// Status å�ªä¼šå‡ºçŽ°åœ¨è¿™å„¿
				if (str != "Status")
					strParse += strSep;
				else
					strParse += strEnter;
			}
		}
		strAdmitDateTime += strSep;
		strActualDischargeDateTime += strSep;
		strCompletedDate += strSep;
		
		// case 3: new tables
		for (String str : newTable)
		{
			Pattern p = Pattern.compile("<[^<A-Z]*" + str + ">(((?!" + str + ").)*)</[^>A-Z]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			// need to parse the next level xml file
			if (str == "DiagnosisList")
			{
				while (m.find())
				{
					int i = 1;
					String strNextLevelXML = m.group(i);
					strParseDiag += strCaseID + strAdmitDateTime + strActualDischargeDateTime + strEHIDSStatus + strCompletedDate;
					strParseDiag += ParseNextLevel(strNextLevelXML, str);
					i++;
				}
			}
			else if (str == "CareProviderInvolvement")
			{
				while (m.find())
				{
					int i = 1;
					String strNextLevelXML = m.group(i);
					strParseCP += strCaseID + strAdmitDateTime + strActualDischargeDateTime + strEHIDSStatus + strCompletedDate;
					strParseCP += ParseNextLevel(strNextLevelXML, str);
					i++;
				}
			}
			else if (str == "MedicationDetails")
			{
				while (m.find())
				{
					int i = 1;
					String strNextLevelXML = m.group(i);
					strParseMed += strCaseID + strAdmitDateTime + strActualDischargeDateTime + strEHIDSStatus + strCompletedDate;
					strParseMed += ParseNextLevel(strNextLevelXML, str);
					i++;
				}
			}
		}

		WriteFile(fwDS, strParse);
		WriteFile(fwDiag, strParseDiag);
		WriteFile(fwCP, strParseCP);
		WriteFile(fwMed, strParseMed);
	}
	
	public String ParseNextLevel (String strDiagXML, String tag)
	{
		String strParseNL = "";
		String[] strLabels = new String[10];
		if (tag == "DiagnosisList")
			strLabels = diagnosis;
		else if (tag == "CareProviderInvolvement" || tag == "CreateBy")
			strLabels = careProviderInvolvement;
		else if (tag == "MedicationDetails")
			strLabels = medications;
		else if (tag == "PrincipalProcedure")
			strLabels = principalProcedure;
		else if (tag == "OutcomeOnDischarge" || tag =="ComplicationsAndComorbidities" || tag == "DischargeOutcome" ||
				tag == "EHIDSStatus" || tag == "MedicalService" || tag == "LeaveType")
			strLabels = sharedChild;
		for (String str : strLabels)
		{
			Pattern p;
			if (str == "PerformedBy")
			{
				p = Pattern.compile("<[^<]*" + str + ">(.*)<[^<]*" + str + ">", Pattern.DOTALL);
			}
			else
			{
				p = Pattern.compile("<[^<]*" + str + ">([^<>]*)<[^<]*" + str + ">", Pattern.DOTALL);
			}
			Matcher m = p.matcher(strDiagXML);
			
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			if ((tag == "DiagnosisList" && str == "DiagnosisComments")
					|| (tag == "CareProviderInvolvement" && str == "CareProviderRoleDescription")
					|| (tag == "MedicationDetails" && str == "DosageRegimen"))
			{
				strParseNL += strEnter;
			}
			else 
			{
				strParseNL += strSep;
			}
		}
		return strParseNL;
	}
	
	public static void main(String[] args) throws Exception
    {
		RE t = new RE();
		
		//long startTime, endTime;
		//startTime=System.currentTimeMillis();
		String strdir = "C:/Users/kpzheng/Desktop/20151128/re_extract/";
		t.ParseDir(strdir);
		//endTime=System.currentTimeMillis();
		//System.out.println("time cost " + (endTime - startTime) + "ms");
    }
}
