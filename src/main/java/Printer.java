/**
 * Created by huimin on 9/28/15.
 */
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.util.ArrayList;

public class Printer
{
    public static StringBuilder output = new StringBuilder();
    public static StringBuilder relation = new StringBuilder();
    public static ArrayList<String> dependent = new ArrayList<String>();
    public static ArrayList<String> interfaces = new ArrayList<String>();
    public static ArrayList<String> classes = new ArrayList<String>();
    public static ArrayList<String> uses = new ArrayList<String>();
    public static ArrayList<String> implementation = new ArrayList<String>();

    public static void main(String[] args) throws Exception
    {
        File folder = new File("/users/huimin/Desktop/uml-parser-test-1");
        File[] listOfFiles = folder.listFiles();

        FileInputStream in;
        CompilationUnit cu;

        //build the interfaces and classes arraylists
        for (int i = 0; i < listOfFiles.length; i++)
        {
            File file = listOfFiles[i];
            if (file.isFile() && file.getName().endsWith(".java"))
            {
                in = new FileInputStream(file);
                try
                {
                    // parse the file
                    cu = JavaParser.parse(in);
                }
                finally
                {
                    in.close();
                }

                new ClassVisitor().visit(cu, null);
            }
        }
        System.out.println(interfaces);
        System.out.println(classes);

        for (int i = 0; i < listOfFiles.length; i++)
        {
            File file = listOfFiles[i];
            if (file.isFile() && file.getName().endsWith(".java"))
            {
                in = new FileInputStream(file);
                try
                {
                    cu = JavaParser.parse(in);
                }
                finally
                {
                    in.close();
                }

                // visit and print the methods names
                new ClassVisitor().visit(cu, null);
                new MethodVisitor().visit(cu, null);
                new FieldVisitor().visit(cu, null);
                //System.out.print("}"+"\n");
                output.append("}"+"\n");

                //add the class name to those "ignored" relationship
                int indexofclass = relation.toString().indexOf("\"");
                int indexofline = relation.toString().indexOf("\n"+"\"");

                while(indexofline >= 0 && indexofline <= relation.toString().lastIndexOf("\n"+"\""))
                {
                    //System.out.println(indexofline);
                    relation.insert(indexofline+1,relation.toString().substring(0, indexofclass));
                    indexofline = relation.toString().indexOf("\n", indexofline+1);
                }
                //System.out.println(relation.toString().substring(0,indexofclass));
            }
        }



        OutputStream png = new FileOutputStream("/users/huimin/Desktop/test1.png");
        String source = "@startuml" + "\n";

        //dealing with association relationship
        //System.out.println(relation.toString());
        if(relation.toString().contains("-"))
        {
            ArrayList<String> removeduplicate = new ArrayList<String>();
            //System.out.println(relation.toString());
            for (String str : relation.toString().split("\n"))
            {

                int firstindex = str.indexOf("\"");
                int lastindex = str.lastIndexOf("\"");
                if (firstindex > 0 && lastindex > 0)
                {
                    String front = str.substring(0, firstindex);
                    String middle = str.substring(firstindex, lastindex+1);
                    String end = str.substring(lastindex+1, str.length());
                    /**for (int i = 0; (4*i + 2) < implementation.size();i++)    //for dependency pointing to interfaces
                    {
                        if (implementation.get(4*i + 2).equals(front))
                        {
                            front = implementation.get(4*i);
                        }
                        if (implementation.get(4*i + 2).equals(end))
                        {
                            end = implementation.get(4*i);
                        }
                     }*/
                    if(front.compareTo(end) > 0)
                    {
                        String reverse = new StringBuffer(middle).reverse().toString();
                        str = end + reverse + front;
                    }
                }

                //System.out.println(str);
                if(str.contains("-") && !removeduplicate.contains(str))
                {
                    removeduplicate.add(str);
                }
            }

            for(int i = 0; i < removeduplicate.size(); i++)
            {
                for(int j = i+1; j < removeduplicate.size(); j++)
                {
                    String compare1 = removeduplicate.get(i);
                    String compare2 = removeduplicate.get(j);
                    int compare1index1 = compare1.indexOf("\"");
                    int compare1index2 = compare1.lastIndexOf("\"");
                    int compare2index1 = compare2.indexOf("\"");
                    int compare2index2 = compare2.lastIndexOf("\"");
                    if( compare1index1 > 0 && compare1index2 > 0 && compare2index1 > 0 && compare2index2 >0 &&
                            compare1.substring(0,compare1index1).equals(compare2.substring(0,compare2index1)) &&
                            compare1.substring(compare1index2+1,compare1.length()).
                                    equals(compare2.substring(compare2index2 + 1, compare2.length())))
                    {
                        if(compare1.contains("*") && (!compare2.contains("*")))
                        {
                            removeduplicate.remove(j);
                            j--;
                        }
                        else if((!compare1.contains("*")) && compare2.contains("*"))
                        {
                            removeduplicate.remove(i);
                            i--;
                        }
                        else
                        {
                            removeduplicate.remove(j);
                            j--;
                            removeduplicate.remove(i);
                            i--;
                            removeduplicate.add(compare1.substring(0,compare1index1) + "\"*\"--\"*\"" +
                                    compare1.substring(compare1index2+1, compare1.length()));
                        }
                    }
                }
            }

            for(String print : removeduplicate)
            {
                System.out.println(print); //check the relationships
                source += print + "\n";
            }
            //System.out.println(removeduplicate);
        }

        //dealing with dependency relationship
        for (int i = 0; i < uses.size(); i++)
        {
            if (interfaces.contains(uses.get(i)))
            {
                String dependency = uses.get(i-1) + RelationSymbol.dependency.toString() + uses.get(i) + " : uses" + "\n";
                if (!dependent.contains(dependency))
                {
                    dependent.add(dependency);
                }

            }
        }


        System.out.println(output.toString());   //check the classes
        //System.out.println(interfaces.toString());
        //System.out.println(classes.toString());
        System.out.println(implementation.toString().substring(1,implementation.toString().length()-1).replace(", ",""));
        System.out.println(dependent.toString().substring(1,dependent.toString().length()-1).replace(", ", ""));     //check the dependency relationship
        source += implementation.toString().substring(1,implementation.toString().length()-1).replace(", ","");
        source += dependent.toString().substring(1,dependent.toString().length()-1).replace(", ","");
        source += output.toString() + "\n";
        source += "\n"+"@enduml\n";
        SourceStringReader reader = new SourceStringReader(source);
        String desc = reader.generateImage(png);

    }


    private static class ClassVisitor extends VoidVisitorAdapter
    {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg)
        {
            if (n.isInterface())
            {
                if (interfaces.contains(n.getName()))
                {
                    if(n.getExtends() != null)
                    {
                        output.append(n.getName() + RelationSymbol.extension.toString());
                        output.append(n.getExtends().toString().substring(1,n.getExtends().toString().length()-1) + "\n");
                    }
                    if(n.getImplements() != null)
                    {
                        String imple = n.getImplements().toString().substring(1,n.getImplements().toString().length()-1);
                        String[] impleArray = imple.split(",");
                        for(int i = 0 ; i < impleArray.length;i++){
                            //output.append(n.getName() + RelationSymbol.implement.toString());
                            //output.append(impleArray[i] + "\n");
                            implementation.add(n.getName());
                            implementation.add(RelationSymbol.implement.toString());
                            implementation.add(impleArray[i]);
                            implementation.add("\n");
                        }
                    }

                    output.append("interface ");
                    output.append(n.getName() + " {" + "\n");
                    if (n.getName().length() == 1)
                    {
                        relation.append(n.getName());
                    }
                    else
                    {
                        relation.append("\n" + n.getName());
                    }
                }
                else
                {
                    interfaces.add(n.getName());
                }
            }
            else
            {
                if (classes.contains(n.getName()))
                {
                    if(n.getExtends() != null)
                    {
                        output.append(n.getName() + RelationSymbol.extension.toString());
                        output.append(n.getExtends().toString().substring(1,n.getExtends().toString().length()-1) + "\n");
                    }
                    if(n.getImplements() != null)
                    {
                        String imple = n.getImplements().toString().substring(1,n.getImplements().toString().length()-1);
                        String[] impleArray = imple.split(",");
                        for(int i = 0 ; i < impleArray.length;i++){
                            //output.append(n.getName() + RelationSymbol.implement.toString());
                            //output.append(impleArray[i] + "\n");
                            implementation.add(n.getName());
                            implementation.add(RelationSymbol.implement.toString());
                            implementation.add(impleArray[i]);
                            implementation.add("\n");
                        }
                    }

                    output.append("class ");
                    uses.add(n.getName());
                    output.append(n.getName() + " {" + "\n");
                    if (n.getName().length() == 1)
                    {
                        relation.append(n.getName());
                    }
                    else
                    {
                        relation.append("\n" + n.getName());
                    }
                }
                else
                {
                    classes.add(n.getName());
                }
            }
        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter
    {
        @Override
        public void visit(MethodDeclaration n, Object arg)
        {
            //System.out.print(n.getModifiers());
            //System.out.println(n.getType() + n.getName() + n.getParameters());
            //only consider public, public static, public abstract methods
            if (n.getModifiers() == 1 || n.getModifiers() == 9 || n.getModifiers() == 1025)
            {
                if ( n.getParameters().toString().equals("[]"))
                {
                    output.append("+" + n.getName() + "() : " + n.getType() + "\n");
                }
                else
                {
                    //System.out.println(n.getParameters()); -- eg. [String msg]
                    int space = n.getParameters().toString().indexOf(" ");
                    //System.out.println(space);
                    String useinterface = "";
                    if (space > 0)
                    {
                        //System.out.println(n.getType() + n.getName() + n.getParameters());
                        useinterface = n.getParameters().toString().substring(1,space);
                        if (classes.contains(useinterface) || interfaces.contains(useinterface))
                        {
                            uses.add(useinterface);
                        }

                        String length = n.getParameters().toString();
                        output.append("+" + n.getName() + "(" + length.substring(space + 1,length.length()-1)
                                    + " : " + length.substring(1,space) + ") : " + n.getType() + "\n");
                    }
                }
            }
            //output.append(n.getType()+" "+n.getName() + "\n");
        }

        @Override
        public void visit(ConstructorDeclaration n, Object arg)     //get constructors
        {
            //System.out.println(n.getDeclarationAsString());
            if (n.getModifiers() == 1)       //public constructor
            {
                if ( n.getParameters().toString().equals("[]"))
                {
                    output.append("+" + n.getName() + "()" + "\n");
                }
                else
                {
                    int space = n.getParameters().toString().indexOf(" ");
                    String useinterface = "";
                    if (space > 0)
                    {
                        useinterface = n.getParameters().toString().substring(1,space);
                        if (classes.contains(useinterface) || interfaces.contains(useinterface))
                        {
                            uses.add(useinterface);
                        }
                        String length = n.getParameters().toString();
                        output.append("+" + n.getName() + "(" + length.substring(space + 1,length.length()-1)
                                    + " : " + length.substring(1,space) + ")" + "\n");

                    }
                }
            }
            else if (n.getModifiers() == 2)       //private constructor
            {
                if ( n.getParameters().toString().equals("[]"))
                {
                    output.append("-" + n.getName() + "()" + "\n");
                }
                else
                {
                    int space = n.getParameters().toString().indexOf(" ");
                    if (space > 0)
                    {
                        String length = n.getParameters().toString();
                        output.append("-" + n.getName() + "(" + length.substring(space + 1,length.length()-1)
                                + " : " + length.substring(1,space) + ")" + "\n");

                    }
                }
            }
        }


    }

    private static class FieldVisitor extends VoidVisitorAdapter
    {
        @Override
        public void visit(FieldDeclaration n, Object arg)
        {
            if (n.getType() == null)
            {
            }
            else
            {
                if (n.getType().toString().contains("<"))     //find "< >" and replace with "*"
                {
                    String fieldtype = n.getType().toString();
                    String symbol = fieldtype.substring(fieldtype.indexOf("<")+1,fieldtype.indexOf(">"));
                    if (classes.contains(symbol) || interfaces.contains(symbol))
                    {
                        relation.append("\" \"" + RelationSymbol.association.toString()+"\"*\"" + symbol + "\n");
                    }
                }
                else
                {
                    if (classes.contains(n.getType().toString()) || interfaces.contains(n.getType().toString()))
                    {
                        relation.append("\" \"" + RelationSymbol.association.toString() + "\" \"" + n.getType()+"\n");
                    }
                    else
                    {
                        if (n.getModifiers() == 1)  //public
                        {
                            output.append("+");
                            output.append(n.getVariables().toString().substring(1, n.getVariables().toString().length()-1)
                                    + " : " + n.getType() + "\n");
                        }
                        else if (n.getModifiers() == 2)     //private
                        {
                            output.append("-");
                            output.append(n.getVariables().toString().substring(1, n.getVariables().toString().length()-1)
                                    + " : " + n.getType() + "\n");
                        }
                    }
                }
            }
            //relation.append(n.getType() + "\n");
        }

        @Override       //read the local variables
        public void visit(VariableDeclarationExpr n, Object arg)
        {
            //System.out.println(n.getType() + "22222222222");
            //System.out.println(n.getVars() + "3333333333");
            if (classes.contains(n.getType().toString()) || interfaces.contains(n.getType().toString()))
            {
                uses.add(n.getType().toString());
            }
        }
    }

}



