/**
 * Created by huimin on 9/28/15.
 */
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.sun.org.apache.xpath.internal.operations.Mod;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class Printer
{
    public static ArrayList<String> output = new ArrayList<String>();
    //public static StringBuilder output = new StringBuilder();
    public static StringBuilder relation = new StringBuilder();
    public static ArrayList<String> dependent = new ArrayList<String>();
    public static ArrayList<String> interfaces = new ArrayList<String>();
    public static ArrayList<String> classes = new ArrayList<String>();
    public static ArrayList<String> uses = new ArrayList<String>();
    public static ArrayList<String> implementation = new ArrayList<String>();
    public static ArrayList<String> extend = new ArrayList<String>();

    public static void main(String[] args) throws Exception
    {
        if (args.length < 2)
        {
            System.out.println("Please input 2 arguments!");
        }
        String folderpath = args[0];
        String umlname = args[1];

        File folder = new File(folderpath);
        //File folder = new File("/users/huimin/Desktop/uml-parser-test-5");
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
        //System.out.println(interfaces);
        //System.out.println(classes);

        OutputStream png = new FileOutputStream(folderpath + "/" + umlname + ".png");
        //OutputStream png = new FileOutputStream("/users/huimin/Desktop/test5.png");
        String source = "@startuml" + "\n";

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
                //output.append("}"+"\n");
                output.add("}");
                output.add("\n");

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

                //dealing with dependency relationship
                if (uses.size() > 1 && classes.contains(uses.get(0)))
                {
                    for (int j = 1; j < uses.size(); j++)
                    {
                        String dependency = uses.get(0) + RelationSymbol.dependency.toString() + uses.get(j) + " : uses" + "\n";
                        if (!dependent.contains(dependency))
                        {
                            dependent.add(dependency);
                        }
                    }
                }
                uses.clear();

                //dealing with methods, constructors and variables inside a class or interface
                if (output.get(0).equals("interface "))
                {
                    for (int k = 0; k < output.size(); k++)
                    {
                        if (output.get(k).contains(":") && (!output.get(k).contains("(")))   //check if it has variable, delete it
                        {
                            output.remove(k);
                            output.remove(k - 1);
                        }
                        else if (output.get(k).contains(") :"))  //check if it has method, change all methods to public
                        {
                            if (output.get(k - 1).equals("-") || output.get(k - 1).equals("#") || output.get(k - 1).equals("~"))
                            {
                                output.set(k - 1, "+");
                            }
                        }
                    }
                }
                else if (output.get(0).equals("class "))
                {
                    for (int k = 0; k < output.size(); k++)
                    {
                        if (output.get(k).contains(") :") && (output.get(k-1).equals("-") || output.get(k-1).equals("#")
                                || output.get(k-1).equals("~")))  //check if it has method, delete those not public
                        {
                            output.remove(k);
                            output.remove(k-1);
                            k = k - 2;
                        }

                        //dealing with getter and setter
                        if (output.get(k).contains(":") && (!output.get(k).contains("("))) //check variables
                        {
                            int comma = output.get(k).indexOf(":");
                            String var = output.get(k).substring(0, comma - 1);
                            int set = 0;
                            int get = 0;
                            for (int l = 0; l < output.size(); l++)
                            {
                                if (output.get(l).contains(") :")) //check methods
                                {
                                    int half = output.get(l).indexOf("(");
                                    String method = output.get(l).substring(0, half);
                                    if (method.equals("set" + var.substring(0, 1).toUpperCase() + var.substring(1, var.length()))
                                            && output.get(l - 1).equals("+"))
                                    {
                                        set = l;
                                    }
                                    if (method.equals("get" + var.substring(0, 1).toUpperCase() + var.substring(1, var.length()))
                                            && output.get(l - 1).equals("+"))
                                    {
                                        get = l;
                                    }
                                }
                            }

                            if (set != 0 && get != 0)
                            {
                                output.set(k - 1, "+");
                                if (set > get)
                                {
                                    output.remove(set);
                                    output.remove(set - 1);
                                    output.remove(get);
                                    output.remove(get - 1);
                                }
                                else
                                {
                                    output.remove(get);
                                    output.remove(get - 1);
                                    output.remove(set);
                                    output.remove(set - 1);
                                }
                                k = k - 4;
                            }
                        }
                    }
                }

                //System.out.println(output.toString().substring(1,output.toString().length()-1).replace(", ", ""));
                source += output.toString().substring(1,output.toString().length()-1).replace(", ","");
                output.clear();
            }
        }


        //dealing with association relationship
        if(relation.toString().contains("-"))
        {
            ArrayList<String> removeduplicate = new ArrayList<String>();
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
                //System.out.println(print); //check the relationships
                source += print + "\n";
            }
        }



        //System.out.println(implementation.toString().substring(1,implementation.toString().length()-1).replace(", ", ""));
        //System.out.println(extend.toString().substring(1,extend.toString().length()-1).replace(", ", ""));
        //System.out.println(dependent.toString().substring(1,dependent.toString().length()-1).replace(", ", ""));     //check the dependency relationship


        source += implementation.toString().substring(1,implementation.toString().length()-1).replace(", ","");
        source += extend.toString().substring(1,extend.toString().length()-1).replace(", ","");
        source += dependent.toString().substring(1,dependent.toString().length()-1).replace(", ","");
        source += "\n" + "title Huimin Jian 010129561\n" + "@enduml\n";
        SourceStringReader reader = new SourceStringReader(source);
        String desc = reader.generateImage(png);
        System.out.println("UML diagram is generated: " + folderpath + "/" + umlname + ".png");
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
                    uses.add(n.getName());
                    if(n.getExtends() != null)
                    {
                        extend.add(n.getName());
                        extend.add(RelationSymbol.extension.toString());
                        extend.add(n.getExtends().toString().substring(1,n.getExtends().toString().length()-1));
                        extend.add("\n");
                    }
                    if(n.getImplements() != null)
                    {
                        String imple = n.getImplements().toString().substring(1,n.getImplements().toString().length()-1);
                        String[] impleArray = imple.split(",");
                        for(int i = 0 ; i < impleArray.length;i++)
                        {
                            implementation.add(n.getName());
                            implementation.add(RelationSymbol.implement.toString());
                            implementation.add(impleArray[i]);
                            implementation.add("\n");
                        }
                    }

                    output.add("interface ");
                    output.add(n.getName());
                    output.add(" {");
                    output.add("\n");

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
                    uses.add(n.getName());
                    if(n.getExtends() != null)
                    {
                        extend.add(n.getName());
                        extend.add(RelationSymbol.extension.toString());
                        extend.add(n.getExtends().toString().substring(1,n.getExtends().toString().length()-1));
                        extend.add("\n");
                    }
                    if(n.getImplements() != null)
                    {
                        String imple = n.getImplements().toString().substring(1,n.getImplements().toString().length()-1);
                        String[] impleArray = imple.split(",");
                        for(int i = 0 ; i < impleArray.length;i++)
                        {
                            implementation.add(n.getName());
                            implementation.add(RelationSymbol.implement.toString());
                            implementation.add(impleArray[i]);
                            implementation.add("\n");
                        }
                    }

                    output.add("class ");
                    output.add(n.getName());
                    output.add(" {");
                    output.add("\n");

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
            if (Modifier.isPublic(n.getModifiers()))
            {
                output.add("+");
            }
            else if (Modifier.isPrivate(n.getModifiers()))
            {
                output.add("-");
            }
            else if (Modifier.isProtected(n.getModifiers()))
            {
                output.add("#");
            }
            else
            {
                output.add("~");
            }

            if ( n.getParameters().toString().equals("[]"))
            {
                output.add(n.getName() + "(" + ")" + " : " + n.getType().toString() + "\n");
            }
            else
            {
                //System.out.println(n.getParameters()); -- eg. [String msg]
                int space = n.getParameters().toString().indexOf(" ");
                if (space > 0)
                {
                    //System.out.println(n.getType() + n.getName() + n.getParameters());
                    String useinterface = n.getParameters().toString().substring(1,space);
                    if (interfaces.contains(useinterface))
                    {
                        uses.add(useinterface);
                    }

                    String length = n.getParameters().toString();
                    output.add(n.getName() + "(" + length.substring(space + 1,length.length()-1) + " : " +
                            length.substring(1,space) + ")" + " : " + n.getType().toString() + "\n");
                }
            }
        }

        @Override
        public void visit(ConstructorDeclaration n, Object arg)     //get constructors
        {
            //System.out.println(n.getDeclarationAsString());
            if (Modifier.isPublic(n.getModifiers()))
            {
                output.add("+");
            }
            else if (Modifier.isPrivate(n.getModifiers()))
            {
                output.add("-");
            }
            else if (Modifier.isProtected(n.getModifiers()))
            {
                output.add("#");
            }
            else
            {
                output.add("~");
            }

            if ( n.getParameters().toString().equals("[]"))
            {
                output.add(n.getName() + "()" + "\n");
            }
            else
            {
                int space = n.getParameters().toString().indexOf(" ");
                if (space > 0)
                {
                    String useinterface = n.getParameters().toString().substring(1,space);
                    if (interfaces.contains(useinterface))
                    {
                        uses.add(useinterface);
                    }
                    String length = n.getParameters().toString();
                    output.add(n.getName() + "(" + length.substring(space + 1,length.length()-1) + " : " +
                            length.substring(1,space) + ")" + "\n");
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
                            output.add("+");
                            output.add(n.getVariables().toString().substring(1, n.getVariables().toString().length()-1)
                             + " : " + n.getType().toString() + "\n");
                        }
                        else if (n.getModifiers() == 2)     //private
                        {
                            output.add("-");
                            output.add(n.getVariables().toString().substring(1, n.getVariables().toString().length()-1)
                             + " : " + n.getType().toString() + "\n");
                        }
                    }
                }
            }
            //relation.append(n.getType() + "\n");
        }

        @Override       //read the local variables
        public void visit(VariableDeclarationExpr n, Object arg)
        {
            //System.out.println(n.getType());
            //System.out.println(n.getVars());
            if (interfaces.contains(n.getType().toString()))
            {
                uses.add(n.getType().toString());
            }
        }
    }

}
