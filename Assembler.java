import java.util.Scanner;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.Hashtable;

class Assembler{   
    public static Hashtable<String,String> opTable;
    public static Hashtable<Integer,Line> line;
    public static Hashtable<Integer,String> errLog;
    public static Hashtable<String,String> symbolTable;
    public static Hashtable<String,LinkedList<Integer>> symbolUn;
    public static int errID;
    public static int lineID;
    public static String progName;
    public static int startAdd;
    public static int endAdd;
    public static String length;
    public static String firstAdd;
    public static void main(String[] argv) throws IOException{
        opTable = new Hashtable<String,String>();
        line = new Hashtable<Integer,Line>();
        errLog = new Hashtable<Integer,String>();
        symbolTable = new Hashtable<String,String>();
        symbolUn = new Hashtable<String,LinkedList<Integer>>();
        errID = 0;
        lineID = 0;
        Scanner input = new Scanner(System.in);
        Creat_opTable();
        Read(input.next());
        CheckSymbol(); 
        ShowErr();
        if(errLog.size()==0){
            Write();
            ShowRecord();
        }      
    }

    static void Creat_opTable() throws IOException{
        FileReader fr = new FileReader("opCode.txt");
        BufferedReader reader = new BufferedReader(fr);
        String s;

        while ( (s = reader.readLine()) != null ){
            String[] arys = s.split(" ");
            opTable.put(arys[0],arys[1]);
        }
    }
    static boolean Ismnemonic(String word){
        String tmp = opTable.get(word);
        if (tmp!=null || word.equals("WORD") || word.equals("BYTE") || word.equals("RESW") || word.equals("RESB") 
            ||word.equals("START")||word.equals("END"))
            return true;
        else
            return false;
    }

    static String findOpcode(String word){
        String tmp = opTable.get(word);
        return tmp;
    }

    static void Read(String fileName) throws IOException{
        FileReader fr = new FileReader(fileName);
        BufferedReader reader = new BufferedReader(fr);
        String string;
        int lineId = 0;

        int counter=0;
        int currentSize=0;
        boolean isStart=false;
        boolean isEnd=false;     
        while((string = reader.readLine())!= null)
        {
            if (isEnd){
                errID++;
                errLog.put(errID,"Line "+lineId+": END要在最後一行");
            }
            lineId++;
            String[] tmpString = string.trim().split("\\s+");          
            if (tmpString[0].equals("")||tmpString[0].substring(0,1).equals(".")){
                continue;
            }
            else{
                int mneIndex=-1;
                int dotIndex=tmpString.length;
                for (int i = 0;i < tmpString.length;i++)
                {   
                    if(Ismnemonic(tmpString[i])){
                        mneIndex = i;
                        break;
                    }
                }
                for (int i = 0;i < tmpString.length;i++)
                {
                    if(tmpString[i].substring(0,1).equals(".")){
                        dotIndex = i;
                        break;
                    }
                }
                
                int caseNum = -1;
                if (mneIndex==-1){
                    caseNum = 1;                  
                }else if (tmpString.length>1&&mneIndex == 0 && Ismnemonic(tmpString[mneIndex+1]) ){
                    caseNum = 2;                   
                }else if (tmpString.length>2&&mneIndex == 1 && Ismnemonic(tmpString[mneIndex+1]) ){
                    caseNum = 2;
                }else if (tmpString.length>2&&mneIndex == 2  ){
                    caseNum = 2;
                }
                else{
                    caseNum = 3;    
                } 
                switch(caseNum){
                    case 1:
                        errID++;
                        errLog.put(errID,"Line "+lineId+": lack of mnemonic");
                        break;
                    case 2:
                        errID++;
                        errLog.put(errID,"Line "+lineId+": symbol or operand can't  be mnemonic");
                        break;
                    case 3:
                        Line newLine = new Line();
                        lineID++;
                        newLine.lineId = lineId;
                        newLine.mnemonic = tmpString[mneIndex];
                        newLine.opCode = findOpcode(newLine.mnemonic);
                        if (mneIndex ==1)
                            newLine.label = tmpString[0];
                        //LDA QWE QWE QWE QWE;
                        for(int i = mneIndex+1;i<dotIndex;i++){
                            newLine.operand += tmpString[i];
                            if(tmpString[i].equals("C'")){
                                newLine.operand +=" ";
                            }
                            //System.out.println(tmpString[mneIndex+1]);
                        }
                        line.put(lineID,newLine);

                        if (newLine.mnemonic.equals("START")){
                            if(isStart){
                                errID++;
                                errLog.put(errID,"Line "+newLine.lineId+": 只能有一個START");
                            }else{
                                if(lineID!=1){
                                    errID++;
                                    errLog.put(errID,"START要在首行");
                                }
                                try
                                {
                                    progName = newLine.label;
                                    newLine.loc=newLine.operand;
                                    counter = Integer.parseInt(newLine.operand,16);
                                
                                    startAdd = counter; 
                                    currentSize = 0;
                                    isStart = true;   
                                }
                                catch (NumberFormatException e)
                                {
                                    errID++;
                                    errLog.put(errID,"Line "+newLine.lineId+": START後之operand 只能是16進位");
                                }    
                            }

                        }else{
                            counter += currentSize;
                            newLine.loc = IntToHex(counter);
                            //System.out.println(newLine.loc);
                            if(!newLine.label.equals("")){
                                if(symbolTable.get(newLine.label)!=null){
                                    errID++;
                                    errLog.put(errID,"Line "+newLine.lineId+": Symbol重複定義");
                                    
                                }else{
                                    symbolTable.put(newLine.label,newLine.loc);
                                    newLine.update=true;
                                    //if(symbolUn.get(newLine.label)!=null){
                                    //    LinkedList<Integer> tmp = new LinkedList<Integer>();
                                    //}
                                }
                            }

                            newLine.obCode += newLine.opCode;
                            
                            if(opTable.get( newLine.mnemonic)==null ){//特殊mnemonic
                                
                                if(newLine.mnemonic.equals("RESW")){
                                    try
                                    {
                                        String str = newLine.operand;
                                        int a = Integer.parseInt(str);
                                        currentSize = a*3;
                                        //System.out.println(newLine.obCode);
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        errLog.put(errID,"Line "+newLine.lineId+": RESW之oprand 只能是整數");
                                    }
                                }else if(newLine.mnemonic.equals("RESB")){
                                    try
                                    {
                                        String str = newLine.operand;
                                        int a = Integer.parseInt(str);
                                        currentSize = a;
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        errLog.put(errID,"Line "+newLine.lineId+": RESB之oprand 只能是整數");
                                    }
                                }else if(newLine.mnemonic.equals("WORD")){
                                    try
                                    {
                                        String str = newLine.operand;
                                        int a = Integer.parseInt(str);
                                        currentSize = 3;
                                        newLine.obCode ="000000".substring(0, 6 - IntToHex(a).length())+IntToHex(a);
                                        //System.out.println(newLine.obCode);
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        errLog.put(errID,"Line "+newLine.lineId+": WORD之oprand 只能是整數");
                                    }
                                }else if(newLine.mnemonic.equals("BYTE")){
                                    if(newLine.operand.substring(0,2).equals("C\'")&&
                                        newLine.operand.substring(newLine.operand.length()-1,newLine.operand.length()).equals("'"))
                                        {
                                        newLine.obCode="";
                                        for(int i =2;i<newLine.operand.length()-1;i++){
                                            char c = newLine.operand.charAt(i);
                                            int a = (int) c; 
                                            newLine.obCode += IntToHex(a);
                                        }
                                        currentSize = (newLine.operand.length()-3);
                                    }else if(newLine.operand.substring(0,2).equals("X'")&&
                                        newLine.operand.substring(newLine.operand.length()-1,newLine.operand.length()).equals("'")&&
                                        (newLine.operand.length()-3)%2==0){
                                        try
                                        {
                                            String str = newLine.operand.substring(2,newLine.operand.length()-1);
                                            int a = Integer.parseInt(str,16);
                                            newLine.obCode =str;
                                            currentSize = (newLine.operand.length()-3)/2;
                                            
                                        }
                                        catch (NumberFormatException e)
                                        {
                                            errID++;
                                            errLog.put(errID,"Line "+newLine.lineId+":BYTE之 oprand 只能是16進位");
                                        }    
                                    }else{
                                        errID++;
                                        errLog.put(errID,"Line "+newLine.lineId+": BYTE之 oprand格式錯誤");
                                    }
                                }else if(newLine.mnemonic.equals("END")){
                                    if(isEnd){
                                        errID++;
                                        errLog.put(errID,"Line "+newLine.lineId+": 只能有一個END");
                                    }else{
                                        isEnd = true;
                                        endAdd = counter;
                                        if(symbolTable.get(newLine.operand)!=null){//有在                               
                                            firstAdd = newLine.operand;
                                            length = IntToHex(endAdd- startAdd);
                                        }
                                        else{
                                            errID++;
                                            errLog.put(errID,"Line "+lineId+":未定義SYMBOL:"+newLine.operand);
                                        }                                       
                                    }
                                    
                                }
                            }else if(newLine.mnemonic.equals("RSUB")){
                                if(!newLine.operand.equals("")){
                                    errID++;
                                    errLog.put(errID,"Line "+newLine.lineId+":RSUB不能有OPErand");
                                }else{
                                    currentSize = 3;
                                    newLine.obCode = newLine.opCode;
                                    newLine.obCode+="0000";
                                }
                            }

                            else{
                                
                                currentSize = 3;
                                String tmp = newLine.operand;
                                newLine.obCode = newLine.opCode;
                                try{
                                    if(tmp.substring(tmp.length()-1,tmp.length()).equals("X")){

                                        if(tmp.substring(tmp.length()-2,tmp.length()-1).equals(",")){

                                            newLine.isIndexAdd=true;
                                            if(symbolTable.get(tmp.substring(0,tmp.length()-2))!=null){//有在symbol
                                                String a = symbolTable.get(tmp.substring(0,tmp.length()-2));
                                                int b = Integer.parseInt(a,16);
                                                int x = Integer.parseInt("8000",16);
                                                String c = IntToHex(b+x);
                                                newLine.obCode += c;
                                                
                                            }
                                            else{
                                                newLine.obCode += "0000";
                                                if(symbolUn.get(tmp.substring(0,tmp.length()-2))==null){

                                                    symbolUn.put(tmp.substring(0,tmp.length()-2),new LinkedList<Integer>());
                                                    symbolUn.get(tmp.substring(0,tmp.length()-2)).add(counter);

                                                }else{
                                                    symbolUn.get(tmp.substring(0,tmp.length()-2)).add(counter);
                                                }
                                            }
                                        }
                                        else{
                                            errID++;
                                            errLog.put(errID,"Line "+newLine.lineId+": 索引定址格式錯誤");
                                        }

                                    }
                                    else{
                                        if(symbolTable.get(tmp)!=null){//有在
                        
                                                String a = symbolTable.get(tmp.substring(0,tmp.length()));
                                                newLine.obCode += "0000".substring(0, 4 - a.length())+a;            
                                        }
                                        else{
                                            newLine.obCode += "0000";
                                            if(symbolUn.get(tmp)==null){

                                                symbolUn.put(tmp,new LinkedList<Integer>());
                                                symbolUn.get(tmp).add(counter);

                                            }else{
                                                symbolUn.get(tmp).add(counter);
                                            }
                                        }

                                    }
                                }
                                catch(StringIndexOutOfBoundsException e)
                                {

                                }
                            }

                        }
                        break;
                }
            //System.out.println(newLine.label+"-"+newLine.mnemonic+"-"+newLine.operand);
            //System.out.println(newLine.operand);
                
            }
         }//END WHILE 
        if (!isStart){
            errID++;
            errLog.put(errID,"開頭要有START");
        }
        if (!isEnd){
            errID++;
            errLog.put(errID,"結尾要有END");
        }     
    }  

    static void  Write() throws IOException{
        File f = new File("record.txt");//建立輸出結果檔案
        FileWriter fw = new FileWriter(f);
        BufferedWriter writer = new BufferedWriter(fw);

        String start ="";
        String context="";
        for (int id=1;id<=lineID;id++){

            Line tmpLine= new Line(); 
            tmpLine=line.get(id);
            if(tmpLine.mnemonic.equals("START")){
                writer.write("H"+progName+("      ".substring(0, 6 - progName.length()))+("000000".substring(0, 6 - tmpLine.operand.length()))+tmpLine.operand+
                    ("000000".substring(0, 6 - length.length())+length)); 
                writer.newLine();
            }else if(tmpLine.mnemonic.equals("END" )){
                if(!context.equals("")){
                    writer.write(("00".substring(0, 2 - IntToHex(context.length()/2).length()))+IntToHex(context.length()/2)+context);
                    writer.newLine();
                    context = "";
                }
                if(tmpLine.update&&symbolUn.get(tmpLine.label)!=null){
                    LinkedList<Integer> tmp = new LinkedList<Integer>();
                    tmp =symbolUn.get(tmpLine.label);
                    for(int i = 0;i<tmp.size();i++){
                        String update = IntToHex(tmp.get(i)+1);
                        writer.write("T00"+update+"02"+tmpLine.loc);
                        writer.newLine();
                    }
                    
                }
                String a = symbolTable.get(tmpLine.operand);
                writer.write("E"+("000000".substring(0, 6 - a.length())+a));
                writer.close();
                return;

            }else{
                if(!tmpLine.obCode.equals(null)&&!tmpLine.obCode.equals("null")){
                    if(tmpLine.update&&symbolUn.get(tmpLine.label)!=null){
                        if(!context.equals("")){
                            writer.write(("00".substring(0, 2 - IntToHex(context.length()/2).length()))+IntToHex(context.length()/2)+context);
                            writer.newLine();
                            context = "";
                        }
                        LinkedList<Integer> tmp = new LinkedList<Integer>();
                        tmp =symbolUn.get(tmpLine.label);
                        for(int i = 0;i<tmp.size();i++){
                            String update = IntToHex(tmp.get(i)+1);
                            writer.write("T00"+update+"02"+tmpLine.loc);
                            writer.newLine();
                        }
                    }
                    if(context.length()==0){
                        writer.write("T00"+tmpLine.loc);
                        context = context+tmpLine.obCode;
                    }
                    else{
                        String x=line.get(id).obCode;
                        context=context+x;
                        
                        if(context.length()>60){
                            context = context.substring(0,context.length()-x.length());
                            writer.write(IntToHex(context.length()/2)+context);
                            context = "";
                            writer.newLine();
                            writer.write("T00"+tmpLine.loc);
                            context = context+tmpLine.obCode;
                        }
                        
                    }
                }
                
            }
            //System.out.println(tmpLine.loc+" "+tmpLine.label+" "+tmpLine.mnemonic+" "+tmpLine.operand+" "+tmpLine.obCode);
        }
    }

    static String IntToHex(int number){
        String hex = Integer.toHexString(number);
        return hex;
    }

    static void ShowErr(){
        System.out.println(errID+" error");
        for(int i = 1;i<=errID;i++){
            System.out.println(errLog.get(i));
        }
    }

    static void CheckSymbol(){
        if(symbolUn.size()==0){
            ;
        }
        else{
            for(Object key:symbolUn.keySet()){
                if(symbolTable.get(key)==null){
                    errID++;
                    errLog.put(errID,"未定義SYMBOL:"+key);
                }
            }

        }
    }
    static void ShowRecord() throws IOException{
        FileReader fr = new FileReader("record.txt");
        BufferedReader reader = new BufferedReader(fr);
        String s;

        while ( (s = reader.readLine()) != null ){
            System.out.println(s);
        }
    }
}

class Line{
    int lineId;
    String label;
    String mnemonic;
    String operand;
    boolean isIndexAdd;
    String loc;
    String opCode;
    String obCode;
    boolean update;
    public Line(){
        lineId = 0;
        label = "";
        mnemonic = "";
        operand = "";
        isIndexAdd = false;
        loc="";
        obCode="";
        update = false;
   } 
}