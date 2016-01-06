// extract proc table

package proc_extract;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.regex.*;

public class PROCEXTRACT {
	public static String strSep = "*";
	public static String strEnter = "\n";
	
	public static String[] hasTail = {"MessageID", "MessageTimestamp", "EventType", "TxnTimestamp", "PatientUID", 
			"StartTimestamp", "QueueName", "AdditionalInfo", "SemanticGroup", "BirthDateTime", "GenderCode", "GenderDesc",
			"CaseTypeCode", "CaseTypeDescription", "SpecialtyCode", "SpecialtyDescription", "ServiceOrderIdentificationNumber",
			"CareProviderInvolvement", "ProcedureBeginDateTime", "ProcedureCatalogCode", "ProcedureCatalogDescription",
			"ProcedureCode", "ProcedureCodeDescription", "ProcedureMainCode", "NumberOfProcedures", "ProcedureEndDateTime",
			"OrderCatalogCode", "OrderCatalogDescription", "OrderCode", "OrderCodeDescription", "OrderQuantityAmount",
			"ChangedOn", "ChangedBy", "DRGSequenceNumber", "DRGCategory", "DRGCategoryDescription", "ProcedureTypeCode",
			"OrderCategoryCode", "OrderCategoryDescription",	"ProcedureTypeDescription", "CreatedOn", "CreatedBy", 
			"PlannedProcedure", "TypeOfOrder", "Complications", "DRGIndicator"};
	
	public static String[] noTail = {"IDNumber", "PatientIdentificationNumber", "LocalPatientIdentificationNumber", 
			"CaseIdentificationNumber", "OrderIdentificationNumber"};
	//CareProviderInvolvement
	public static String[] careProviderInvolvement = {"LicenseNumber", "Name", "CareProviderRoleCode", "CareProviderRoleDescription"};
	//CreateBy and ChangedBy
	public static String name = "Name";
	//PlannedProcedure
	public static String quantityAmount = "OrderQuantityAmount";
	
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
		String strLTFile = "Procedure.txt";
		String strLogFile = "Log.txt";
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		
		FileWriter fwSP = new FileWriter(strdir + strLTFile);
		FileWriter fwLog = new FileWriter(strdir + strLogFile);
		
		PrintSchema(fwSP);
		
		int i = 1;
		for (File f: files)
		{
			String s = f.getPath();
			strXML = ReadFile(s);
			Parse(strXML, fwSP);
			WriteFile(fwLog, s + " processed . Row " + Integer.toString(i) + ".\n");
			fwLog.flush();
			//System.out.println(s + " has been processed successfully. Row " + Integer.toString(i) + " in the DS file.");
			i++;
		}	
		endTime=System.currentTimeMillis();
		WriteFile(fwLog, "time cost " + (endTime - startTime) + "ms.\n");
		//System.out.println("time cost " + (endTime - startTime) + "ms");
		
		fwSP.close();
		fwLog.close();
	}

	public void PrintSchema(FileWriter fwSP) throws Exception
	{
		String SchemaSP = "";
		
		for (String str: noTail)
		{
			SchemaSP += (str + strSep);
		}
		for (String str: hasTail)
		{
			if (str == "CareProviderInvolvement")
			{
				SchemaSP += (careProviderInvolvement[0] +  strSep);
				SchemaSP += (careProviderInvolvement[1] +  strSep);
				SchemaSP += (careProviderInvolvement[2] +  strSep);
				SchemaSP += (careProviderInvolvement[3] +  strSep);
			}
			else if (str == "CreatedBy")
				SchemaSP += ("CreatedBy Name" + strSep);
			else if (str == "ChangedBy")
				SchemaSP += ("ChangedBy Name" + strSep);
			else if (str == "PlannedProcedure")
				SchemaSP += ("PlannedProcedure " + quantityAmount + strSep);
			else
			{
				if (str == hasTail[hasTail.length - 1])
					SchemaSP += (str + strEnter);
				else
					SchemaSP += (str + strSep);
			}
		}
		WriteFile(fwSP, SchemaSP);
	}
	public void Parse(String strXML, FileWriter fwSP) throws Exception
	{
		String strParse = "";
		
		// case 1: patterns with no tails
		for (String str : noTail)
		{
			Pattern p = Pattern.compile("<[^>]*" + str + " extension=\"([^\"]*)\"/>", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				strParse += m.group(1);
			}
			strParse += strSep;
		}
				
		// case 2: patterns with tails
		for (String str : hasTail)
		{
			//Pattern p = Pattern.compile("<[^A-Z]*" + str + ".*>(((?!" + str + ").)*)</[^A-Z]*" + str + ">", Pattern.DOTALL);
			Pattern p = Pattern.compile("<[^<A-Z]*" + str + "[^>]*>(((?!" + str + ").)*)</[^>A-Z]*" + str + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (str == "CareProviderInvolvement")
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
					strParse += (strSep + strSep + strSep + strSep);
				}
			}
			else if (str == "ChangedBy"||str == "CreatedBy" || str == "PlannedProcedure")
			{
				if (m.find())
				{
					String strNextLevelXML = m.group(1);
					String tmp = ParseNextLevel(strNextLevelXML, str);
					strParse += tmp;	
				}
				else
				{
					strParse += strSep;
				}
			}
			else
			{
				if (m.find())
				{
					strParse += m.group(1);
				}
				// for the last one, add a '\n';
				if (str != hasTail[hasTail.length - 1])
					strParse += strSep;
				else
					strParse += strEnter;
			}
		}
		
		WriteFile(fwSP, strParse);
	}
	
	public String ParseNextLevel (String strXML, String tag)
	{
		String strParseNL = "";
		
		if (tag == "CareProviderInvolvement")
		{
			for (String str : careProviderInvolvement)
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
		else if (tag == "ChangedBy"||tag == "CreatedBy") 
		{
			Pattern p = Pattern.compile("<[^<]*" + name + ">(.*)<[^<]*" + name + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			strParseNL += strSep;
		}
		else if (tag == "PlannedProcedure")
		{
			Pattern p = Pattern.compile("<[^<]*" + quantityAmount + ">(.*)<[^<]*" + quantityAmount + ">", Pattern.DOTALL);
			Matcher m = p.matcher(strXML);
			if (m.find())
			{
				strParseNL += m.group(1);
			}
			strParseNL += strSep;
		}
		return strParseNL;
	}
	
	public static void main(String[] args) throws Exception
    {
		PROCEXTRACT t = new PROCEXTRACT();
		
		long startTime, endTime;
		startTime=System.currentTimeMillis();
		String strdir = "/home/xr/data/export1115/SP_MSG/";
		t.ParseDir(strdir);
		endTime=System.currentTimeMillis();
		System.out.println("time cost " + (endTime - startTime) + "ms");
    }
}
