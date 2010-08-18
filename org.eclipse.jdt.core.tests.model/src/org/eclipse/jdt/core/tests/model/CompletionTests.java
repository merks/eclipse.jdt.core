/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.eval.EvaluationContextWrapper;

public class CompletionTests extends AbstractJavaModelCompletionTests {

static {
//	TESTS_NAMES = new String[] { "testDeprecationCheck17"};
}
public static Test suite() {
	return buildModelTestSuite(CompletionTests.class);
}
public CompletionTests(String name) {
	super(name);
}
public void setUpSuite() throws Exception {
	if (COMPLETION_PROJECT == null)  {
		COMPLETION_PROJECT = setUpJavaProject("Completion");
	} else {
		setUpProjectCompliance(COMPLETION_PROJECT, "1.4");
	}
	super.setUpSuite();
}
private String getVarClassSignature(IEvaluationContext context) {
	char[] varClassName = ((EvaluationContextWrapper)context).getVarClassName();
	return Signature.createTypeSignature(varClassName, true);
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=247941
public void testAbortCompletion1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/AbortCompletion.java",
		"public class AbortCompletion {\n"+
		"	void foo() {\n"+
		"		Objec\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	IProgressMonitor monitor = new NullProgressMonitor();
	try {
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);
		assertResults(
				"Object[TYPE_REF]{Object, java.lang, Ljava.lang.Object;, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} catch (OperationCanceledException e) {
		assertTrue("Should not be cancelled", false);
	}
	
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=247941
public void testAbortCompletion2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/AbortCompletion.java",
		"public class AbortCompletion {\n"+
		"	void foo() {\n"+
		"		Objec\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	
	try {
		IProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true); /*force completion to abort*/
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);
		assertTrue("Should be cancelled", false);
	} catch (OperationCanceledException e) {
		assertResults(
				"",
				requestor.getResults());
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=246276
public void testArrayInitializer1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/ArrayInitializer.java",
		"public class ArrayInitializer {\n"+
		"	int bar() {return 0;}\n"+
		"	void foo(int[] i) {\n"+
		"		i = new int[] {\n"+
		"			bar()\n"+
		"		};\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar(";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	assertResults(
			"bar[METHOD_REF]{, LArrayInitializer;, ()I, bar, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=132679
public void testBug132679() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;\n" +
			"class Context {\n" +
			"\n" +
			"  private int value = 10;\n" +
			"\n" +
			"  public abstract class Callback {\n" +
			"      public abstract void doit(int value);\n" +
			"  }\n" +
			"}\n" +
			"\n" +
			"public class ContextTest {\n" +
			"\n" +
			"  private Context context = new Context();\n" +
			"\n" +
			"  public void test() {\n" +
			"      context.new Callback() {\n" +
			"      public void doit(int value) {\n" +
			"        Object foo = new Object();\n" +
			"        foo.equ\n" +
			"      }\n" +
			"    };\n" +
			"  }\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo.equ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=164311
public void testBug164311() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"    public int zzzzzz;\n" +
		"    public void method1() {\n" +
		"        label : if (0> (10));\n" +
		"        zzz\n" +
		"    }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzzzzz[FIELD_REF]{zzzzzz, Ltest.Test;, I, zzzzzz, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=164311
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=167750
public void testBug164311_2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n"+
		"public class X {\n"+
		"    public void zork() { \n"+
		"    } \n"+
		"    public void foo() { \n"+
		"        this.foo(new Object(){\n"+
		"            public void bar() {\n"+
		"                if (zzz>(Integer)vvv.foo(i)) {\n"+
		"                    return;\n"+
		"                }\n"+
		"                if (true) {\n"+
		"                    return;\n"+
		"                }\n"+
		"                zor\n"+
		"            }        \n"+
		"        });\n"+
		"    }\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zork[METHOD_REF]{zork(), Ltest.X;, ()V, zork, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=96213
public void testBug96213() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n" +
            "public class Test{\n"+
            "  Test toto(Object o) {\n"+
            "    return null;\n"+
            "  }\n"+
            "  void titi(int removed) {\n"+
            "  }\n"+
            "  void foo() {\n"+
            "    int removed = 0;\n"+
            "    toto(Test.this).titi(removed);\n"+
            "  }\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "removed";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
            "removed[LOCAL_VARIABLE_REF]{removed, null, I, removed, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=99811
public void testBug99811() throws JavaModelException {
	ICompilationUnit aType = null;
    try {
    	this.wc = getWorkingCopy(
	            "/Completion/src/test/A.java",
	            "public abstract class A implements I {}");

	    aType = getWorkingCopy(
	            "/Completion/src/test/I.java",
	            "public interface I {\n"+
	            "  public class M extends A {}\n"+
	            "}");

	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "A";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults("", requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=169682
public void testBug169682a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	
	String source =
		"package test;\n"+
		"\n"+
		"public class Test \n"+
		"{\n"+
		"        public Test() \n"+
		"        {\n"+
		"                this.t// do ctrl+space here\n"+
		"        }\n"+
		"\n"+
		"        Object field = new Object() \n"+
		"        {\n"+
		"                public void foo() {\n"+
		"\n"+
		"                        if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {}\n"+
		"                        else if(true)\n"+
		"                        {\n"+
		"                                if(true)\n"+
		"                                {\n"+
		"                                        if(true)\n"+
		"                                        {\n"+
		"                                                boolean result[][];\n"+
		"                                        }\n"+
		"                                }\n"+
		"                        }       \n"+
		"                }\n"+
		"        };   \n"+   
		"}";
	
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		source);
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.t";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=169682
public void testBug169682b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	
	String source =
		"package test;\n"+
		"public class Test\n"+
		"{\n"+
		"        #\n"+
		"        int[] i;\n"+
		"        Obj x; // do ctrl+space at |\n"+
		"}";
	
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		source);
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Obj";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Obj[POTENTIAL_METHOD_DECLARATION]{Obj, Ltest.Test;, ()V, Obj, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Object[TYPE_REF]{Object, java.lang, Ljava.lang.Object;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=275518
public void testBug275518a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	
	String source =
		"package test;\n"+
		"public class Test {\n"+
		"\n"+
		"        void v() {\n"+
		"\n"+
		"        }\n"+
		"\n"+
		"}";
	
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		source);
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "void v() {";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"v[METHOD_REF]{v(), Ltest.Test;, ()V, v, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=275518
public void testBug275518b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	
	String source =
		"package test;\n"+
		"public class Test {\n"+
		"\n"+
		"        void v() { \n"+
		"\n"+
		"        }\n"+
		"\n"+
		"}";
	
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		source);
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "void v() {";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"v[METHOD_REF]{v(), Ltest.Test;, ()V, v, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=275518
public void testBug275518c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	
	String source =
		"package test;\n"+
		"public class Test {\n"+
		"\n"+
		"        void v() { \n"+
		"\n"+
		"        }\n"+
		"\n"+
		"}";
	
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		source);
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "void v() { ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"v[METHOD_REF]{v(), Ltest.Test;, ()V, v, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseField1() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  int oneTwoThree;\n"+
			"  int oTTField;\n"+
			"  void foo() {\n"+
			"    oTT\n"+
			"  }\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "oTT";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"oneTwoThree[FIELD_REF]{oneTwoThree, Lcamelcase.Test;, I, oneTwoThree, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"oTTField[FIELD_REF]{oTTField, Lcamelcase.Test;, I, oTTField, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseLocalVariable1() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  void foo() {\n"+
			"    int oneTwoThree;\n"+
			"    int oTTLocal;\n"+
			"    oTT\n"+
			"  }\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "oTT";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"oneTwoThree[LOCAL_VARIABLE_REF]{oneTwoThree, null, I, oneTwoThree, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"oTTLocal[LOCAL_VARIABLE_REF]{oTTLocal, null, I, oTTLocal, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseMethod1() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

	this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  void oneTwoThree(){}\n"+
			"  void oTTMethod(){}\n"+
			"  void foo() {\n"+
			"    oTT\n"+
			"  }\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "oTT";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"oneTwoThree[METHOD_REF]{oneTwoThree(), Lcamelcase.Test;, ()V, oneTwoThree, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"oTTMethod[METHOD_REF]{oTTMethod(), Lcamelcase.Test;, ()V, oTTMethod, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseMethodDeclaration1() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test extends SuperClass {\n"+
			"  oTT\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/camelcase/SuperClass.java",
			"package camelcase;"+
			"public class SuperClass {\n"+
			"  public void oneTwoThree(){}\n"+
			"  public void oTTMethod(){}\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "oTT";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"oTT[POTENTIAL_METHOD_DECLARATION]{oTT, Lcamelcase.Test;, ()V, oTT, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"oneTwoThree[METHOD_DECLARATION]{public void oneTwoThree(), Lcamelcase.SuperClass;, ()V, oneTwoThree, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"oTTMethod[METHOD_DECLARATION]{public void oTTMethod(), Lcamelcase.SuperClass;, ()V, oTTMethod, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseType1() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[3];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  FF\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/camelcase/FoFoFo.java",
			"package camelcase;"+
			"public class FoFoFo {\n"+
			"}");

		this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/camelcase/FFFTest.java",
			"package camelcase;"+
			"public class FFFTest {\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "FF";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"FF[POTENTIAL_METHOD_DECLARATION]{FF, Lcamelcase.Test;, ()V, FF, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"FoFoFo[TYPE_REF]{FoFoFo, camelcase, Lcamelcase.FoFoFo;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"FFFTest[TYPE_REF]{FFFTest, camelcase, Lcamelcase.FFFTest;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseType2() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[3];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  camelcase.FF\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/camelcase/FoFoFo.java",
			"package camelcase;"+
			"public class FoFoFo {\n"+
			"}");

		this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/camelcase/FFFTest.java",
			"package camelcase;"+
			"public class FFFTest {\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "FF";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"FoFoFo[TYPE_REF]{FoFoFo, camelcase, Lcamelcase.FoFoFo;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_NON_RESTRICTED) + "}\n" +
				"FFFTest[TYPE_REF]{FFFTest, camelcase, Lcamelcase.FFFTest;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseType3() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  /**/FF\n"+
			"}\n"+
			"class FoFoFo {\n"+
			"}\n"+
			"class FFFTest {\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "/**/FF";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"FF[POTENTIAL_METHOD_DECLARATION]{FF, Lcamelcase.Test;, ()V, FF, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"FoFoFo[TYPE_REF]{FoFoFo, camelcase, Lcamelcase.FoFoFo;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"FFFTest[TYPE_REF]{FFFTest, camelcase, Lcamelcase.FFFTest;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseType4() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[3];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  FF\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/camelcase/Member1.java",
			"package camelcase;"+
			"public class Member1 {\n"+
			"  public class FoFoFo {\n"+
			"  }\n"+
			"}");

		this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/camelcase/Member2.java",
			"package camelcase;"+
			"public class Member2 {\n"+
			"  public class FFFTest {\n"+
			"  }\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "FF";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"FF[POTENTIAL_METHOD_DECLARATION]{FF, Lcamelcase.Test;, ()V, FF, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"Member1.FoFoFo[TYPE_REF]{camelcase.Member1.FoFoFo, camelcase, Lcamelcase.Member1$FoFoFo;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_NON_RESTRICTED) + "}\n" +
				"Member2.FFFTest[TYPE_REF]{camelcase.Member2.FFFTest, camelcase, Lcamelcase.Member2$FFFTest;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102572
public void testCamelCaseType5() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/camelcase/Test.java",
			"package camelcase;"+
			"public class Test {\n"+
			"  public class FoFoFo {\n"+
			"    public class FFFTest {\n"+
			"      FF\n"+
			"    }\n"+
			"  }\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "FF";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"FF[POTENTIAL_METHOD_DECLARATION]{FF, Lcamelcase.Test$FoFoFo$FFFTest;, ()V, FF, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"Test.FoFoFo[TYPE_REF]{FoFoFo, camelcase, Lcamelcase.Test$FoFoFo;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"Test.FoFoFo.FFFTest[TYPE_REF]{FFFTest, camelcase, Lcamelcase.Test$FoFoFo$FFFTest;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing()\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZAException[TYPE_REF]{IZZAException, test, Ltest.IZZAException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef03() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      #\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZAException[TYPE_REF]{IZZAException, test, Ltest.IZZAException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef04() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws test.p.IZZAException, test.p.IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (test.p.IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/p/IZZAException.java",
			"package test.p;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/p/IZZBException.java",
			"package test.p;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/p/IZZException.java",
			"package test.p;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{test.p.IZZBException, test.p, Ltest.p.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{test.p.IZZException, test.p, Ltest.p.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef05() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public class IZZAException extends Exception {}\n" +
		"	public class IZZBException extends Exception {}\n" +
		"	public class IZZException extends Exception {}\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test.IZZBException[TYPE_REF]{IZZBException, test, Ltest.Test$IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"Test.IZZException[TYPE_REF]{IZZException, test, Ltest.Test$IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef06() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"   public class Inner {\n" +
		"      public class IZZAException extends Exception {}\n" +
		"      public class IZZBException extends Exception {}\n" +
		"      public class IZZException extends Exception {}\n" +
		"      public void throwing() throws IZZAException, IZZException {}\n" +
		"      public void foo() {\n" +
		"         try {\n" +
		"            throwing();\n" +
		"         }\n" +
		"         catch (IZZAException e) {\n" +
		"            bar();\n" +
		"         }\n" +
		"         catch (IZZ) {\n" +
		"         }\n" +
		"      }" +
		"   }" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test.Inner.IZZBException[TYPE_REF]{IZZBException, test, Ltest.Test$Inner$IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"Test.Inner.IZZException[TYPE_REF]{IZZException, test, Ltest.Test$Inner$IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef07() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"   void zork() {\n" +
		"      class IZZAException extends Exception {}\n" +
		"      class IZZBException extends Exception {}\n" +
		"      class IZZException extends Exception {}\n" +
		"      class Local {\n" +
		"         public void throwing() throws IZZAException, IZZException {}\n" +
		"         public void foo() {\n" +
		"            try {\n" +
		"               throwing();\n" +
		"            }\n" +
		"            catch (IZZAException e) {\n" +
		"               bar();\n" +
		"            }\n" +
		"            catch (IZZ) {\n" +
		"            }\n" +
		"         }" +
		"      }" +
		"   }" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, LIZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, LIZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef08() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (/**/) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Exception[TYPE_REF]{Exception, java.lang, Ljava.lang.Exception;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXPECTED_TYPE + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef09() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[5];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZCException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         try {\n" +
		"            throwing();\n" +
		"         }\n" +
		"         catch (IZZCException e) {\n" +
		"            bar();\n" +
		"         }\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZCException.java",
			"package test;"+
			"public class IZZCException extends Exception {\n" +
			"}\n");

	this.workingCopies[4] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZCException[TYPE_REF]{IZZCException, test, Ltest.IZZCException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef10() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends IZZBException {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXPECTED_TYPE + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
//IZZBException should not be proposed but to filter this proposal
//we would need to know subclasses of IZZAException and it's currenlty too costly to compute
public void testCatchClauseExceptionRef11() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"         bar();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends IZZAException {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef12() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZAException, IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZAException[TYPE_REF]{IZZAException, test, Ltest.IZZAException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157584
public void testCatchClauseExceptionRef13() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZBException.java",
			"package test;"+
			"public class IZZBException extends Exception {\n" +
			"}\n");

	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends IZZAException {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}\n" +
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCatchClauseExceptionRef13b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"class IZZAException extends Exception {\n" +
		"}\n" +
		"class IZZBException extends Exception {\n" +
		"}\n" +
		"class IZZException extends IZZAException {\n" +
		"}\n" +
		"public class Test {\n" +
		"	public void throwing() throws IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZBException[TYPE_REF]{IZZBException, test, Ltest.IZZBException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCatchClauseExceptionRef14() throws JavaModelException {

	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         throwing();\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}" +
		"class IZZAException extends Exception {\n" +
		"}" +
		"class IZZException extends Exception {\n" +
		"}\n");

	IJavaProject project = this.workingCopies[0].getJavaProject();

	try {
		project.setOption(JavaCore.CODEASSIST_VISIBILITY_CHECK, JavaCore.ENABLED);

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "(IZZ";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_EXCEPTION + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		project.setOption(JavaCore.CODEASSIST_VISIBILITY_CHECK, null);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=173907
public void testCatchClauseExceptionRef15() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public void throwing() throws IZZException, IZZAException {}\n" +
		"	public void foo() {\n" +
		"      try {\n" +
		"         try {\n" +
		"            throwing();\n" +
		"         } finally {}\n" +
		"      }\n" +
		"      catch (IZZAException e) {\n" +
		"      }\n" +
		"      catch (IZZ) {\n" +
		"      }\n" +
		"   }" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/IZZAException.java",
			"package test;"+
			"public class IZZAException extends Exception {\n" +
			"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/IZZException.java",
			"package test;"+
			"public class IZZException extends Exception {\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "IZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"IZZException[TYPE_REF]{IZZException, test, Ltest.IZZException;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
/*
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=65737
 */
public void testCompletion2InterfacesWithSameMethod() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "Completion2InterfacesWithSameMethod.java");

	String str = cu.getSource();
	String completeBehind = "var.meth";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:method    completion:method()    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionAbstractMethod1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAbstractMethod1.java",
            "public class CompletionAbstractMethod1 {\n" +
            "	abstract class A {\n" +
            "		abstract void foo();\n" +
            "	}\n" +
            "	class B extends A {\n" +
            "		void foo{} {}\n" +
            "		void bar() {\n" +
            "			super.fo\n" +
            "		}\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "",
            requestor.getResults());
}
public void testCompletionAbstractMethod2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAbstractMethod2.java",
            "public class CompletionAbstractMethod2 {\n" +
            "	abstract class A {\n" +
            "		abstract void foo();\n" +
            "	}\n" +
            "	class B extends A {\n" +
            "		void foo{} {}\n" +
            "		void bar() {\n" +
            "			this.fo\n" +
            "		}\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
           "foo[METHOD_REF]{foo(), LCompletionAbstractMethod2$A;, ()V, foo, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED) + "}",
           requestor.getResults());
}
public void testCompletionAbstractMethod3() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAbstractMethod3.java",
            "public class CompletionAbstractMethod3 {\n" +
            "	abstract class A {\n" +
            "		abstract void foo();\n" +
            "	}\n" +
            "	class B extends A {\n" +
            "		void bar() {\n" +
            "			this.fo\n" +
            "		}\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
           "foo[METHOD_REF]{foo(), LCompletionAbstractMethod3$A;, ()V, foo, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED)+"}",
           requestor.getResults());
}
public void testCompletionAbstractMethod4() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAbstractMethod4.java",
            "public class CompletionAbstractMethod1 {\n" +
            "	class A {\n" +
            "		void foo(){}\n" +
            "	}\n" +
            "	abstract class B extends A {\n" +
            "		abstract void foo();\n" +
            "	}\n" +
            "	class C extends B {\n" +
            "		void foo{} {}\n" +
            "		void bar() {\n" +
            "			super.fo\n" +
            "		}\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
           "",
           requestor.getResults());
}

/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25578
*/
public void testCompletionAbstractMethodRelevance1() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionAbstractSuperClass.java",
	            "public abstract class CompletionAbstractSuperClass {\n"+
	            "	public void foo1(){}\n"+
	            "	public abstract void foo2();\n"+
	            "	public void foo3(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionAbstractMethodRelevance1.java",
	            "public class CompletionAbstractMethodRelevance1 extends CompletionAbstractSuperClass {\n"+
	            "	foo\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"foo[POTENTIAL_METHOD_DECLARATION]{foo, LCompletionAbstractMethodRelevance1;, ()V, foo, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"foo1[METHOD_DECLARATION]{public void foo1(), LCompletionAbstractSuperClass;, ()V, foo1, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n" +
			"foo3[METHOD_DECLARATION]{public void foo3(), LCompletionAbstractSuperClass;, ()V, foo3, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n" +
			"foo2[METHOD_DECLARATION]{public void foo2(), LCompletionAbstractSuperClass;, ()V, foo2, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_ABSTRACT_METHOD + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25578
*/
public void testCompletionAbstractMethodRelevance2() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface.java",
	            "public interface CompletionSuperInterface{\n"+
	            "	public int eqFoo(int a,Object b);\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionAbstractMethodRelevance2.java",
	            "public class CompletionAbstractMethodRelevance2 implements CompletionSuperInterface {\n"+
	            "	eq\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "eq";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"eq[POTENTIAL_METHOD_DECLARATION]{eq, LCompletionAbstractMethodRelevance2;, ()V, eq, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n" +
			"eqFoo[METHOD_DECLARATION]{public int eqFoo(int a, Object b), LCompletionSuperInterface;, (ILjava.lang.Object;)I, eqFoo, (a, b), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_ABSTRACT_METHOD + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=211881
public void testCompletionAfterIf1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	void foo(){\n" +
		"		if ((unknown).equals(null)) ;\n" +
		"		int superType = 0;\n" +
		"		superTyp\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "superTyp";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"superType[LOCAL_VARIABLE_REF]{superType, null, I, superType, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=213031
public void testCompletionAfterIf2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	void foo(Object parent){\n" +
		"		/**/eq\n" +
		"		new Object() {\n" +
		"			void bar() {\n" +
		"				if (((Object) parent).equals(parent)) {\n" +
		"				}\n" +
		"			}\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/eq";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionAfterCase1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionAfterCase1.java",
		"public class CompletionAfterCase1 {\n" +
		"	static final int zzz = 5;\n" +
		"	void foo(){\n" +
		"		switch(1) {\n" +
		"			case zz\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz[FIELD_REF]{zzz, LCompletionAfterCase1;, I, zzz, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}",
			requestor.getResults());
}
public void testCompletionAfterCase2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionAfterCase2.java",
		"public class CompletionAfterCase2 {\n" +
		"	static final int zzz = 5;\n" +
		"	void foo(){\n" +
		"		switch(1) {\n" +
		"			case 25:\n" +
		"			case zz\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz[FIELD_REF]{zzz, LCompletionAfterCase2;, I, zzz, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=111882
public void testCompletionAfterCase3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterCase2.java",
		"package test;\n" +
		"public class CompletionAfterCase2 {\n" +
		"	static final int ZZZ1 = 5;\n" +
		"	static final long ZZZ2 = 5;\n" +
		"	static final double ZZZ3 = 0;\n" +
		"	static final Object ZZZ4 = null;\n" +
		"	static final int[] ZZZ5 = null;\n" +
		"	static final Object[] ZZZ6 = null;\n" +
		"	static final short ZZZ7 = 0;\n" +
		"	void foo(int i){\n" +
		"		switch(i) {\n" +
		"			case ZZ\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ2[FIELD_REF]{ZZZ2, Ltest.CompletionAfterCase2;, J, ZZZ2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ3[FIELD_REF]{ZZZ3, Ltest.CompletionAfterCase2;, D, ZZZ3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ4[FIELD_REF]{ZZZ4, Ltest.CompletionAfterCase2;, Ljava.lang.Object;, ZZZ4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ7[FIELD_REF]{ZZZ7, Ltest.CompletionAfterCase2;, S, ZZZ7, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ1[FIELD_REF]{ZZZ1, Ltest.CompletionAfterCase2;, I, ZZZ1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=111882
public void testCompletionAfterCase4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterCase2.java",
		"package test;\n" +
		"public class CompletionAfterCase2 {\n" +
		"	void foo(int i){\n" +
		"		switch(i) {\n" +
		"			case TestConstants.ZZ\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstants.java",
		"package test;\n" +
		"public class TestConstants {\n" +
		"	public static final int ZZZ1 = 5;\n" +
		"	public static final long ZZZ2 = 5;\n" +
		"	public static final double ZZZ3 = 0;\n" +
		"	public static final Object ZZZ4 = null;\n" +
		"	public static final int[] ZZZ5 = null;\n" +
		"	public static final Object[] ZZZ6 = null;\n" +
		"	public static final short ZZZ7 = 0;\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ2[FIELD_REF]{ZZZ2, Ltest.TestConstants;, J, ZZZ2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ3[FIELD_REF]{ZZZ3, Ltest.TestConstants;, D, ZZZ3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ4[FIELD_REF]{ZZZ4, Ltest.TestConstants;, Ljava.lang.Object;, ZZZ4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ7[FIELD_REF]{ZZZ7, Ltest.TestConstants;, S, ZZZ7, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXPECTED_TYPE + R_CASE + R_QUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}\n" +
			"ZZZ1[FIELD_REF]{ZZZ1, Ltest.TestConstants;, I, ZZZ1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_QUALIFIED + R_NON_RESTRICTED + R_FINAL) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=222080
public void testCompletionAfterEqualEqual1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterEqualEqual1.java",
		"package test;\n" +
		"public class CompletionAfterEqualEqual1 {\n" +
		"	void foo(Object a){\n" +
		"		Object zzvar1 = null;\n" +
		"		int zzvar2 = 0;\n" +
		"		if (a == zz) {}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzvar2[LOCAL_VARIABLE_REF]{zzvar2, null, I, zzvar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"zzvar1[LOCAL_VARIABLE_REF]{zzvar1, null, Ljava.lang.Object;, zzvar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=223899
public void testCompletionAfterEqualEqual2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterEqualEqual1.java",
		"package test;\n" +
		"public class CompletionAfterEqualEqual1 {\n" +
		"	void foo(Object a){\n" +
		"		int zzvar1 = 0;\n" +
		"		Object zzvar2 = null;\n" +
		"		int zzvar3 = 0;\n" +
		"		if (a == zz) {}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzvar1[LOCAL_VARIABLE_REF]{zzvar1, null, I, zzvar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"zzvar3[LOCAL_VARIABLE_REF]{zzvar3, null, I, zzvar3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"zzvar2[LOCAL_VARIABLE_REF]{zzvar2, null, Ljava.lang.Object;, zzvar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof02_01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"		if (this.a instanceof CompletionAfterInstanceOf) {\n" +
		"			this.a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("this.a.equal");
	int end2 = start2 + "this.a.equal".length();
	int start3 = str.lastIndexOf("this.a.");
	int end3 = start3 + "this.a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)this.a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof02_02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			this.a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("this.a.equal");
	int end2 = start2 + "this.a.equal".length();
	int start3 = str.lastIndexOf("this.a.");
	int end3 = start3 + "this.a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)this.a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof02_03() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"		if (this.a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof02_04() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof03_01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf)\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof03_02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf)\n" +
		"			bar(a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 + R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof03_03() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		while (true) {\n" +
		"			if (a instanceof CompletionAfterInstanceOf)\n" +
		"				bar(a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 + R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof03_04() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (true) {\n" +
		"			if (a instanceof CompletionAfterInstanceOf)\n" +
		"				bar(a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 + R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof03_05() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) \n" +
		"			while (true)\n" +
		"				a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof03_06() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) \n" +
		"			while (a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int relevance2 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_VOID + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance2) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof04() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			bar(null);\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof05() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			class Z {}\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof06_01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			while (true) {\n" +
		"				a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof06_02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			while (true) \n" +
		"				a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof07() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			if (b instanceof CompletionAfterInstanceOf2) {\n" +
		"				a.equal\n" +
		"	}\n" +
		"}\n" +
		"class CompletionAfterInstanceOf2 {\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof08() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (b instanceof CompletionAfterInstanceOf2) {\n" +
		"			if (a instanceof CompletionAfterInstanceOf) {\n" +
		"				a.equal\n" +
		"	}\n" +
		"}\n" +
		"class CompletionAfterInstanceOf2 {\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof09() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (!(a instanceof CompletionAfterInstanceOf)) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof10() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a, boolean b){\n" +
		"		if (b && (a instanceof CompletionAfterInstanceOf)) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof11() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a, boolean b){\n" +
		"		if (b || (a instanceof CompletionAfterInstanceOf)) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof12() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			#\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof13() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		#\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof14() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			(a).equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof15() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a, Object b){\n" +
		"		if (b instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof16() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	public CompletionAfterInstanceOf b;\n" +
		"	void bar(Object a){\n" +
		"		if (b.a instanceof CompletionAfterInstanceOf) {\n" +
		"			b.a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// unsupported case
public void testCompletionAfterInstanceof17() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public Object a;\n" +
		"	public CompletionAfterInstanceOf b;\n" +
		"	void bar(Object a){\n" +
		"		if (b.a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof18_01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			bar(a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 +  R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof18_02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			new CompletionAfterInstanceOf(){}.bar(a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 + R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof18_03() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a, Object b){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			bar(new CompletionAfterInstanceOf(){}, a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1 + R_VOID) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof19() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a /* comment 1 */\n" +
		"				/* comment 2 */ . /* comment 3 */\n" +
		"					/* comment 4 */ equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a /* comment 1 */");
	int end2 = start2 + "a /* comment 1 */\n\t\t\t\t/* comment 2 */ . /* comment 3 */\n\t\t\t\t\t/* comment 4 */ equal".length();
	int start3 = str.lastIndexOf("a /* comment 1 */");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a) /* comment 1 */\n\t\t\t\t/* comment 2 */ . /* comment 3 */\n\t\t\t\t\t/* comment 4 */ equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof20() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		#\n" +
		"		bar(null);\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo(), Ltest.CompletionAfterInstanceOf;, ()V, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof21() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf)\n" +
		"			bar(null);\n" +
		"		a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof22_01() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			bar(null);\n" +
		"		} else {\n" +
		"			a.equal\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof22_02() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf)\n" +
		"			bar(null);\n" +
		"		else {\n" +
		"			a.equal\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof22_03() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			bar(null);\n" +
		"		} else\n" +
		"			a.equal\n" +
		"		\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// should not return proposals related to instanceof
public void testCompletionAfterInstanceof22_04() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public void equalsFoo(){}\n" +
		"	public void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf)\n" +
		"			bar(null);\n" +
		"		else\n" +
		"			a.equal\n" +
		"		\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof23() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int equalsFoo;\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {\n" +
		"			a.equal\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equal";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("equal") + "".length();
	int end1 = start1 + "equal".length();
	int start2 = str.lastIndexOf("a.equal");
	int end2 = start2 + "a.equal".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"equalsFoo[FIELD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).equalsFoo, Ltest.CompletionAfterInstanceOf;, I, Ltest.CompletionAfterInstanceOf;, equalsFoo, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof24_1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int equalsFoo;\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf[]) {\n" +
		"			a.le\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.le";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("le");
	int end1 = start1 + "le".length();
	int start2 = str.lastIndexOf("a.le");
	int end2 = start2 + "a.le".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"length[FIELD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).length, [Ltest.CompletionAfterInstanceOf;, I, [Ltest.CompletionAfterInstanceOf;, length, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=193909
// supported case
public void testCompletionAfterInstanceof24_2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int equalsFoo;\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf[]) {\n" +
		"			a.cl\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.cl";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("cl") + "".length();
	int end1 = start1 + "cl".length();
	int start2 = str.lastIndexOf("a.cl");
	int end2 = start2 + "a.cl".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	assertResults(
			"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, replace["+start1+", "+end1+"], token["+start1+", "+end1+"], " + (relevance1) + "}\n" +
			"clone[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).clone(), [Ltest.CompletionAfterInstanceOf;, ()Ljava.lang.Object;, [Ltest.CompletionAfterInstanceOf;, clone, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
public void testCompletionAfterSupercall1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionAfterSupercall1.java",
		"public class CompletionAfterSupercall1 extends CompletionAfterSupercall1_1 {\n" +
		"	public void foo(){\n" +
		"		super.foo\n" +
		"	}\n" +
		"}\n" +
		"abstract class CompletionAfterSupercall1_1 extends CompletionAfterSupercall1_2 implements CompletionAfterSupercall1_3 {\n" +
		"	\n" +
		"}\n" +
		"class CompletionAfterSupercall1_2 implements CompletionAfterSupercall1_3 {\n" +
		"	public void foo(){}\n" +
		"}\n" +
		"interface CompletionAfterSupercall1_3 {\n" +
		"	public void foo();\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "super.foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"foo[METHOD_REF]{foo(), LCompletionAfterSupercall1_2;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionAfterSwitch() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAfterSwitch.java");

	String str = cu.getSource();
	String completeBehind = "bar";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:bar    completion:bar()    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_NAME+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionAllMemberTypes() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionAllMemberTypes.java",
            "package test;\n" +
            "public class CompletionAllMemberTypes {\n" +
            "  class Member1 {\n" +
            "    class Member2 {\n" +
            "      class Member3 {\n" +
            "      }\n" +
            "    }\n" +
            "    void foo(){\n" +
            "      Member\n" +
            "    }\n" +
            "  \n}" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "Member";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
            "CompletionAllMemberTypes.Member1.Member2.Member3[TYPE_REF]{test.CompletionAllMemberTypes.Member1.Member2.Member3, test, Ltest.CompletionAllMemberTypes$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes.Member1[TYPE_REF]{Member1, test, Ltest.CompletionAllMemberTypes$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes.Member1.Member2[TYPE_REF]{Member2, test, Ltest.CompletionAllMemberTypes$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}
public void testCompletionAllMemberTypes2() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionAllMemberTypes2.java",
            "package test;\n" +
            "public class CompletionAllMemberTypes2 {\n" +
            "  class Member1 {\n" +
            "    class Member5 {\n" +
            "      class Member6 {\n" +
            "      }\n" +
            "    }\n" +
            "    class Member2 {\n" +
            "      class Member3 {\n" +
            "        class Member4 {\n" +
            "        }\n" +
            "      }\n" +
            "      void foo(){\n" +
            "        Member\n" +
            "      }\n" +
            "    }\n" +
            "  \n}" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "Member";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
            "CompletionAllMemberTypes2.Member1.Member2.Member3.Member4[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member2.Member3.Member4, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3$Member4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member5.Member6[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member5.Member6, test, Ltest.CompletionAllMemberTypes2$Member1$Member5$Member6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1[TYPE_REF]{Member1, test, Ltest.CompletionAllMemberTypes2$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member2[TYPE_REF]{Member2, test, Ltest.CompletionAllMemberTypes2$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member2.Member3[TYPE_REF]{Member3, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member5[TYPE_REF]{Member5, test, Ltest.CompletionAllMemberTypes2$Member1$Member5;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}
public void testCompletionAllMemberTypes3() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionAllMemberTypes2.java",
            "package test;\n" +
            "public interface CompletionAllMemberTypes2 {\n" +
            "  interface Member1 {\n" +
            "    interface Member5 {\n" +
            "      interface Member6 {\n" +
            "      }\n" +
            "    }\n" +
            "    interface Member2 {\n" +
            "      interface Member3 {\n" +
            "        interface Member4 {\n" +
            "        }\n" +
            "      }\n" +
            "        Member\n" +
            "    }\n" +
            "  \n}" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "Member";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
            "Member[POTENTIAL_METHOD_DECLARATION]{Member, Ltest.CompletionAllMemberTypes2$Member1$Member2;, ()V, Member, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member2.Member3.Member4[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member2.Member3.Member4, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3$Member4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member5.Member6[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member5.Member6, test, Ltest.CompletionAllMemberTypes2$Member1$Member5$Member6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1[TYPE_REF]{Member1, test, Ltest.CompletionAllMemberTypes2$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member2[TYPE_REF]{Member2, test, Ltest.CompletionAllMemberTypes2$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member2.Member3[TYPE_REF]{Member3, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionAllMemberTypes2.Member1.Member5[TYPE_REF]{Member5, test, Ltest.CompletionAllMemberTypes2$Member1$Member5;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}
public void testCompletionAllMemberTypes4() throws JavaModelException {
	ICompilationUnit anInterface = null;
	try {
		anInterface = getWorkingCopy(
	            "/Completion/src/test/AnInterface.java",
	            "package test;\n" +
	            "public interface AnInterface {\n" +
	            "  public interface Member1 {\n" +
	            "    public interface Member5 {\n" +
	            "      public interface Member6 {\n" +
	            "      }\n" +
	            "    }\n" +
	            "    public interface Member2 {\n" +
	            "      public interface Member3 {\n" +
	            "        interface Member4 {\n" +
	            "        }\n" +
	            "      }\n" +
	            "        Member\n" +
	            "    }\n" +
	            "  \n}" +
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/CompletionAllMemberTypes2.java",
	            "package test;\n" +
	            "public class CompletionAllMemberTypes2 {\n" +
	            "  class Member1 {\n" +
	            "    class Member5 {\n" +
	            "      class Member6 {\n" +
	            "      }\n" +
	            "    }\n" +
	            "    class Member2 implements AnInterface {\n" +
	            "      class Member3 {\n" +
	            "        class Member4 {\n" +
	            "        }\n" +
	            "      }\n" +
	            "      void foo(){\n" +
	            "        Member\n" +
	            "      }\n" +
	            "    }\n" +
	            "  \n}" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "Member";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    	assertResults(
	            "AnInterface.Member1.Member2[TYPE_REF]{test.AnInterface.Member1.Member2, test, Ltest.AnInterface$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AnInterface.Member1.Member2.Member3[TYPE_REF]{test.AnInterface.Member1.Member2.Member3, test, Ltest.AnInterface$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AnInterface.Member1.Member2.Member3.Member4[TYPE_REF]{test.AnInterface.Member1.Member2.Member3.Member4, test, Ltest.AnInterface$Member1$Member2$Member3$Member4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AnInterface.Member1.Member5[TYPE_REF]{test.AnInterface.Member1.Member5, test, Ltest.AnInterface$Member1$Member5;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AnInterface.Member1.Member5.Member6[TYPE_REF]{test.AnInterface.Member1.Member5.Member6, test, Ltest.AnInterface$Member1$Member5$Member6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1.Member2.Member3.Member4[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member2.Member3.Member4, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3$Member4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1.Member5.Member6[TYPE_REF]{test.CompletionAllMemberTypes2.Member1.Member5.Member6, test, Ltest.CompletionAllMemberTypes2$Member1$Member5$Member6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AnInterface.Member1[TYPE_REF]{Member1, test, Ltest.AnInterface$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1[TYPE_REF]{Member1, test, Ltest.CompletionAllMemberTypes2$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1.Member2[TYPE_REF]{Member2, test, Ltest.CompletionAllMemberTypes2$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1.Member2.Member3[TYPE_REF]{Member3, test, Ltest.CompletionAllMemberTypes2$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionAllMemberTypes2.Member1.Member5[TYPE_REF]{Member5, test, Ltest.CompletionAllMemberTypes2$Member1$Member5;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
	            requestor.getResults());
	} finally {
		if(anInterface != null) {
			anInterface.discardWorkingCopy();
		}
	}
}

public void testCompletionAllMemberTypes5() throws JavaModelException {
	ICompilationUnit aType = null;
	Hashtable oldCurrentOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(oldCurrentOptions);
		options.put(JavaCore.CODEASSIST_VISIBILITY_CHECK, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		aType = getWorkingCopy(
	            "/Completion/src/test/AType.java",
	            "package test;\n" +
	            "public class AType {\n" +
	            "  public class Member1 {\n" +
	            "    private class Member2 {\n" +
	            "      public class Member3 {\n" +
	            "        public class Member4 {\n" +
	            "        }\n" +
	            "      }\n" +
	            "    }\n" +
	            "  \n}" +
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/CompletionAllMemberTypes5.java",
	            "package test;\n" +
	            "public class CompletionAllMemberTypes5 {\n" +
	            "  void foo(){\n" +
	            "    Member\n" +
	            "  }\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "Member";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    	// AType.Member1.Member2.Member3 and AType.Member1.Member2.Member3.Member4 should not be proposed because they are not visible.
    	// But visibility need modifiers of enclosing types to be computed.
    	assertResults(
	            "AType.Member1[TYPE_REF]{test.AType.Member1, test, Ltest.AType$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AType.Member1.Member2.Member3[TYPE_REF]{test.AType.Member1.Member2.Member3, test, Ltest.AType$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AType.Member1.Member2.Member3.Member4[TYPE_REF]{test.AType.Member1.Member2.Member3.Member4, test, Ltest.AType$Member1$Member2$Member3$Member4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
	            requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
		JavaCore.setOptions(oldCurrentOptions);
	}
}

public void testCompletionAllMemberTypes6() throws JavaModelException {
	Hashtable oldCurrentOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(oldCurrentOptions);
		options.put(JavaCore.CODEASSIST_VISIBILITY_CHECK, JavaCore.ENABLED);
		JavaCore.setOptions(options);

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/CompletionAllMemberTypes6.java",
	            "package test;\n" +
	            "class AType {\n" +
	            "  public class Member1 {\n" +
	            "    private class Member2 {\n" +
	            "      public class Member3 {\n" +
	            "      }\n" +
	            "    }\n" +
	            "  }\n" +
	            "}\n" +
	            "public class CompletionAllMemberTypes6 {\n" +
	            "  void foo(){\n" +
	            "    Member\n" +
	            "  }\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "Member";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    	assertResults(
	            "AType.Member1[TYPE_REF]{test.AType.Member1, test, Ltest.AType$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
	            requestor.getResults());
	} finally {
		JavaCore.setOptions(oldCurrentOptions);
	}
}

public void testCompletionAllMemberTypes7() throws JavaModelException {
	Hashtable oldCurrentOptions = JavaCore.getOptions();
	try {
		Hashtable options = new Hashtable(oldCurrentOptions);
		options.put(JavaCore.CODEASSIST_VISIBILITY_CHECK, JavaCore.ENABLED);
		JavaCore.setOptions(options);

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/AType.java",
	            "package test;\n" +
	            "class AType {\n" +
	            "  public class Member1 {\n" +
	            "    private class Member2 {\n" +
	            "      public class Member3 {\n" +
	            "      }\n" +
	            "    }\n" +
	            "  }\n" +
	            "  void foo(){\n" +
	            "    Member\n" +
	            "  }\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "Member";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    	assertResults(
	            "AType.Member1.Member2[TYPE_REF]{test.AType.Member1.Member2, test, Ltest.AType$Member1$Member2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AType.Member1.Member2.Member3[TYPE_REF]{test.AType.Member1.Member2.Member3, test, Ltest.AType$Member1$Member2$Member3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"AType.Member1[TYPE_REF]{Member1, test, Ltest.AType$Member1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
	            requestor.getResults());
	} finally {
		JavaCore.setOptions(oldCurrentOptions);
	}
}

public void testCompletionAllocationExpressionIsParent1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAllocationExpressionIsParent1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionAllocationExpressionIsParent2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAllocationExpressionIsParent2.java",
            "public class CompletionAllocationExpressionIsParent2 {\n" +
            "	public class Inner {\n" +
            "		public Inner(long i, long j){super();}\n" +
            "		public Inner(Object i, Object j){super();}\n" +
            "		\n" +
            "	}\n" +
            "	\n" +
            "	long zzlong;\n" +
            "	int zzint;\n" +
            "	double zzdouble;\n" +
            "	boolean zzboolean;\n" +
            "	Object zzObject;\n" +
            "	\n" +
            "	void foo() {\n" +
            "		this.new Inner(1, zz\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "zz";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "zzObject[FIELD_REF]{zzObject, LCompletionAllocationExpressionIsParent2;, Ljava.lang.Object;, zzObject, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzboolean[FIELD_REF]{zzboolean, LCompletionAllocationExpressionIsParent2;, Z, zzboolean, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzdouble[FIELD_REF]{zzdouble, LCompletionAllocationExpressionIsParent2;, D, zzdouble, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzint[FIELD_REF]{zzint, LCompletionAllocationExpressionIsParent2;, I, zzint, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzlong[FIELD_REF]{zzlong, LCompletionAllocationExpressionIsParent2;, J, zzlong, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
            requestor.getResults());
}

public void testCompletionAllocationExpressionIsParent3() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAllocationExpressionIsParent3.java",
            "public class CompletionAllocationExpressionIsParent3 {\n" +
            "	public class Inner {\n" +
            "		public Inner(long i, long j){super();}\n" +
            "		public Inner(Object i, Object j){super();}\n" +
            "		\n" +
            "	}\n" +
            "	\n" +
            "	long zzlong;\n" +
            "	int zzint;\n" +
            "	double zzdouble;\n" +
            "	boolean zzboolean;\n" +
            "	Object zzObject;\n" +
            "	\n" +
            "	void foo() {\n" +
            "		new CompletionAllocationExpressionIsParent3().new Inner(1, zz\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "zz";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "zzObject[FIELD_REF]{zzObject, LCompletionAllocationExpressionIsParent3;, Ljava.lang.Object;, zzObject, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzboolean[FIELD_REF]{zzboolean, LCompletionAllocationExpressionIsParent3;, Z, zzboolean, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzdouble[FIELD_REF]{zzdouble, LCompletionAllocationExpressionIsParent3;, D, zzdouble, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzint[FIELD_REF]{zzint, LCompletionAllocationExpressionIsParent3;, I, zzint, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzlong[FIELD_REF]{zzlong, LCompletionAllocationExpressionIsParent3;, J, zzlong, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
            requestor.getResults());
}

public void testCompletionAllocationExpressionIsParent4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAllocationExpressionIsParent4.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionAllocationExpressionIsParent5() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAllocationExpressionIsParent5.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionAllocationExpressionIsParent6() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAllocationExpressionIsParent6.java",
            "public class CompletionAllocationExpressionIsParent6 {\n" +
            "	\n" +
            "	long zzlong;\n" +
            "	int zzint;\n" +
            "	double zzdouble;\n" +
            "	boolean zzboolean;\n" +
            "	Object zzObject;\n" +
            "	\n" +
            "	void foo() {\n" +
            "		new CompletionAllocation_ERROR_ExpressionIsParent6Plus().new Inner(1, zz\n" +
            "	}\n" +
            "}\n" +
            "class CompletionAllocationExpressionIsParent6Plus {\n" +
            "	public class Inner {\n" +
            "		public Inner(long i, long j){\n" +
            "			\n" +
            "		}	\n" +
            "	}	\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "zz";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "zzObject[FIELD_REF]{zzObject, LCompletionAllocationExpressionIsParent6;, Ljava.lang.Object;, zzObject, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzboolean[FIELD_REF]{zzboolean, LCompletionAllocationExpressionIsParent6;, Z, zzboolean, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzdouble[FIELD_REF]{zzdouble, LCompletionAllocationExpressionIsParent6;, D, zzdouble, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzint[FIELD_REF]{zzint, LCompletionAllocationExpressionIsParent6;, I, zzint, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
            "zzlong[FIELD_REF]{zzlong, LCompletionAllocationExpressionIsParent6;, J, zzlong, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
            requestor.getResults());
}

public void testCompletionAmbiguousFieldName() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAmbiguousFieldName.java");

	String str = cu.getSource();
	String completeBehind = "xBa";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:xBar    completion:this.xBar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:xBar    completion:xBar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionAmbiguousFieldName2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionAmbiguousFieldName2.java",
            "public class CompletionAmbiguousFieldName2 {\n"+
            "	int xBar;\n"+
            "	class classFoo {\n"+
            "		public void foo(int xBar){\n"+
            "			xBa\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "xBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "xBar[FIELD_REF]{CompletionAmbiguousFieldName2.this.xBar, LCompletionAmbiguousFieldName2;, I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
            "xBar[LOCAL_VARIABLE_REF]{xBar, null, I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
            requestor.getResults());
}

public void testCompletionAmbiguousFieldName3() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAmbiguousFieldName3.java");

	String str = cu.getSource();
	String completeBehind = "xBa";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:xBar    completion:ClassFoo.this.xBar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:xBar    completion:xBar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionAmbiguousFieldName4() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAmbiguousFieldName4.java");

	String str = cu.getSource();
	String completeBehind = "xBa";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:xBar    completion:xBar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionAmbiguousType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAmbiguousType.java");

	String str = cu.getSource();
	String completeBehind = "ABC";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:ABC    completion:p1.ABC    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"\n" +
		"element:ABC    completion:p2.ABC    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionAmbiguousType2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAmbiguousType2.java");

	String str = cu.getSource();
	String completeBehind = "ABC";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:ABC    completion:ABC    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:ABC    completion:p2.ABC    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionArgumentName() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionArgumentName.java");

	String str = cu.getSource();
	String completeBehind = "ClassWithComplexName ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:classWithComplexName    completion:classWithComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:complexName2    completion:complexName2    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:name    completion:name    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:withComplexName    completion:withComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionArrayAccess1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionArrayAccess1.java");

	String str = cu.getSource();
	String completeBehind = "zzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzz1    completion:zzz1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:zzz2    completion:zzz2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE +R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690
public void testCompletionArrayClone() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionArrayClone.java",
            "package test;\n" +
            "public class CompletionArrayClone {\n" +
            "  public void foo() {\n" +
            "    long[] var;\n" +
            "    var.clon\n" +
            "  }\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "clon";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "clone[METHOD_REF]{clone(), [J, ()Ljava.lang.Object;, clone, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=84690
public void testCompletionArrayLength() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionArrayLength.java",
            "package test;\n" +
            "public class CompletionArrayLength {\n" +
            "  public void foo() {\n" +
            "    long[] var;\n" +
            "    var.leng\n" +
            "  }" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "leng";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "length[FIELD_REF]{length, [J, I, length, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}

public void testCompletionArraysCloneMethod() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionArraysCloneMethod.java");

	String str = cu.getSource();
	String completeBehind = ".cl";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:clone    completion:clone()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionAssignmentInMethod1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAssignmentInMethod1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionAssignmentInMethod2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAssignmentInMethod2.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionAssignmentInMethod3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAssignmentInMethod3.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionAssignmentInMethod4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionAssignmentInMethod4.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionBasicAnonymousDeclaration1() throws JavaModelException {
	CompletionResult result = complete(
			"/Completion/src3/test0000/CompletionBasicCompletionContext.java",
			"public class CompletionBasicAnonymousDeclaration1 {\n"+
			"	void foo() {\n"+
			"		new Object(\n"+
			"	}\n"+
			"}",
			"new Object(");

	assertResults(
			"expectedTypesSignatures=null\n" +
			"expectedTypesKeys=null",
			result.context);

	assertResults(
			"Object[ANONYMOUS_CLASS_DECLARATION]{, Ljava.lang.Object;, ()V, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Object[METHOD_REF<CONSTRUCTOR>]{, Ljava.lang.Object;, ()V, Object, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			result.proposals);
}

public void testCompletionBasicCompletionContext() throws JavaModelException {
	CompletionResult result = complete(
			"/Completion/src3/test0000/CompletionBasicCompletionContext.java",
			"package test0000;\n" +
			"public class CompletionBasicCompletionContext {\n" +
			"  void bar(String o) {\n" +
			"    String zzz = null; \n" +
			"    o = zzz\n" +
			"  }\n" +
			"}",
			"zzz");

	assertResults(
			"expectedTypesSignatures={Ljava.lang.String;}\n" +
			"expectedTypesKeys={Ljava/lang/String;}",
			result.context);

	assertResults(
			"zzz[LOCAL_VARIABLE_REF]{zzz, null, Ljava.lang.String;, zzz, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE +  + R_EXACT_NAME + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			result.proposals);
}

public void testCompletionBasicField1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicField1.java",
		"public class CompletionBasicField1 {\n"+
		"	public int zzvarzz;\n"+
		"	void foo() {\n"+
		"		zzvar\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zzvar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("zzvar");
	int rEnd = rStart + "zzvar".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"zzvarzz[FIELD_REF]{zzvarzz, LCompletionBasicField1;, I, zzvarzz, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionBasicKeyword1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicKeyword1.java",
		"public class CompletionBasicKeyword1 {\n"+
		"	void foo() {\n"+
		"		whil\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "whil";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("whil");
	int rEnd = rStart + "whil".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"while[KEYWORD]{while, null, null, while, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionBasicLocalVariable1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicLocalVariable1.java",
		"public class CompletionBasicLocalVariable1 {\n"+
		"	void foo() {\n"+
		"		int zzvarzz;\n"+
		"		zzvar\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zzvar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("zzvar");
	int rEnd = rStart + "zzvar".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"zzvarzz[LOCAL_VARIABLE_REF]{zzvarzz, null, I, zzvarzz, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionBasicMethod1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicMethod1.java",
		"public class CompletionBasicMethod1 {\n"+
		"	void zzfoo() {\n"+
		"		zzfo\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zzfo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("zzfo");
	int rEnd = rStart + "zzfo".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"zzfoo[METHOD_REF]{zzfoo(), LCompletionBasicMethod1;, ()V, zzfoo, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionBasicMethodDeclaration1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicMethodDeclaration1.java",
		"public class CompletionBasicMethodDeclaration1 {\n"+
		"	equals\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "equals";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("equals");
	int rEnd = rStart + "equals".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"equals[POTENTIAL_METHOD_DECLARATION]{equals, LCompletionBasicMethodDeclaration1;, ()V, equals, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_EXACT_NAME + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionBasicPackage1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicPackage1.java",
		"public class CompletionBasicPackage1 {\n"+
		"	java.lan\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "java.lan";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("java.lan");
	int rEnd = rStart + "java.lan".length();

	int tStart = str.lastIndexOf("lan");
	int tEnd = tStart + "lan".length();

	assertResults(
			"java.lang[PACKAGE_REF]{java.lang, java.lang, null, null, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}


public void testCompletionBasicPotentialMethodDeclaration1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicPotentialMethodDeclaration1.java",
		"public class CompletionBasicPotentialMethodDeclaration1 {\n"+
		"	zzpot\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zzpot";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("zzpot");
	int rEnd = rStart + "zzpot".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
			"zzpot[POTENTIAL_METHOD_DECLARATION]{zzpot, LCompletionBasicPotentialMethodDeclaration1;, ()V, zzpot, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}


public void testCompletionBasicType1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicType1.java",
		"public class CompletionBasicType1 {\n"+
		"	void foo() {\n"+
		"		Objec\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("Objec");
	int rEnd = rStart + "Objec".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
		"Object[TYPE_REF]{Object, java.lang, Ljava.lang.Object;, null, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}

public void testCompletionBasicType2() throws JavaModelException {

	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicType2.java",
		"public class CompletionBasicType2 {\n"+
		"	void foo() {\n"+
		"		java.lang.Objec\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("java.lang.Objec");
	int rEnd = rStart + "java.lang.Objec".length();

	int tStart = str.lastIndexOf("Objec");
	int tEnd = tStart + "Objec".length();

	assertResults(
		"Object[TYPE_REF]{Object, java.lang, Ljava.lang.Object;, null, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=207418
public void testCompletionBasicType3() throws JavaModelException {

	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicType3.java",
		"public class CompletionBasicType3 {\n"+
		"	void foo() {\n"+
		"		Objec\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, true, false, true, true, false, true, false);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"",
		requestor.getResults());
}

public void testCompletionBasicVariableDeclaration1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionBasicVariableDeclaration1.java",
		"public class CompletionBasicVariableDeclaration1 {\n"+
		"	public Object obj;\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "obj";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int rStart = str.lastIndexOf("obj");
	int rEnd = rStart + "obj".length();

	int tStart = rStart;
	int tEnd = rEnd;

	assertResults(
		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, replace["+rStart+", "+rEnd+"], token["+tStart+", "+tEnd+"], " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}

public void testCompletionBinaryOperator1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionBinaryOperator1.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var4    completion:var4    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionBinaryOperator2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionBinaryOperator2.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}


public void testCompletionBinaryOperator3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionBinaryOperator3.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}


/**
 * Ensures that completion is not case sensitive
 */
public void testCompletionCaseInsensitive() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu = getCompilationUnit("Completion", "src", "", "CompletionCaseInsensitive.java");

	String str = cu.getSource();
	String completeBehind = "Fiel";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals("should have one class",
		"element:field    completion:field    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_STATIC + R_NON_RESTRICTED),
		requestor.getResults());
}


/**
 * Complete a package in a case insensitive way
 */
public void testCompletionCaseInsensitivePackage() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionCaseInsensitivePackage.java");

	String str = cu.getSource();
	String completeBehind = "Ja";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();

	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have package completions",
		"element:jarpack1    completion:jarpack1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING+ R_NON_RESTRICTED)+"\n" +
		"element:jarpack2    completion:jarpack2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING+ R_NON_RESTRICTED)+"\n" +
		"element:java    completion:java    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"\n" +
		"element:java.io    completion:java.io    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"\n" +
		"element:java.lang    completion:java.lang    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionCastIsParent1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionCastIsParent1.java.java",
		"public class CompletionCastIsParent1 {\n"+
		"	Object zzOb;\n"+
		"	XX00 zz00;\n"+
		"	XX01 zz01;\n"+
		"	XX02 zz02;\n"+
		"	XX10 zz10;\n"+
		"	XX11 zz11;\n"+
		"	XX12 zz12;\n"+
		"	XX20 zz20;\n"+
		"	XX21 zz21;\n"+
		"	XX22 zz22;\n"+
		"	\n"+
		"	Object zzObM(){}\n"+
		"	XX00 zz00M(){}\n"+
		"	XX01 zz01M(){}\n"+
		"	XX02 zz02M(){}\n"+
		"	XX10 zz10M(){}\n"+
		"	XX11 zz11M(){}\n"+
		"	XX12 zz12M(){}\n"+
		"	XX20 zz20M(){}\n"+
		"	XX21 zz21M(){}\n"+
		"	XX22 zz22M(){}\n"+
		"	\n"+
		"	XX11 foo() {\n"+
		"		return (XX11)zz\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zz00[FIELD_REF]{zz00, LCompletionCastIsParent1;, LXX00;, zz00, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz00M[METHOD_REF]{zz00M(), LCompletionCastIsParent1;, ()LXX00;, zz00M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz02[FIELD_REF]{zz02, LCompletionCastIsParent1;, LXX02;, zz02, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz02M[METHOD_REF]{zz02M(), LCompletionCastIsParent1;, ()LXX02;, zz02M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz10[FIELD_REF]{zz10, LCompletionCastIsParent1;, LXX10;, zz10, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz10M[METHOD_REF]{zz10M(), LCompletionCastIsParent1;, ()LXX10;, zz10M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz12[FIELD_REF]{zz12, LCompletionCastIsParent1;, LXX12;, zz12, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz12M[METHOD_REF]{zz12M(), LCompletionCastIsParent1;, ()LXX12;, zz12M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz20[FIELD_REF]{zz20, LCompletionCastIsParent1;, LXX20;, zz20, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz20M[METHOD_REF]{zz20M(), LCompletionCastIsParent1;, ()LXX20;, zz20M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz22[FIELD_REF]{zz22, LCompletionCastIsParent1;, LXX22;, zz22, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz22M[METHOD_REF]{zz22M(), LCompletionCastIsParent1;, ()LXX22;, zz22M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz01[FIELD_REF]{zz01, LCompletionCastIsParent1;, LXX01;, zz01, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz01M[METHOD_REF]{zz01M(), LCompletionCastIsParent1;, ()LXX01;, zz01M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz21[FIELD_REF]{zz21, LCompletionCastIsParent1;, LXX21;, zz21, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz21M[METHOD_REF]{zz21M(), LCompletionCastIsParent1;, ()LXX21;, zz21M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zzOb[FIELD_REF]{zzOb, LCompletionCastIsParent1;, Ljava.lang.Object;, zzOb, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zzObM[METHOD_REF]{zzObM(), LCompletionCastIsParent1;, ()Ljava.lang.Object;, zzObM, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz11[FIELD_REF]{zz11, LCompletionCastIsParent1;, LXX11;, zz11, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"zz11M[METHOD_REF]{zz11M(), LCompletionCastIsParent1;, ()LXX11;, zz11M, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}


public void testCompletionCastIsParent2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionCastIsParent2.java.java",
		"public class CompletionCastIsParent2 {\n"+
		"	XX11 foo() {\n"+
		"		return (XX11)xx\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "xx";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"XX00[TYPE_REF]{XX00, , LXX00;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX01[TYPE_REF]{XX01, , LXX01;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX02[TYPE_REF]{XX02, , LXX02;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX10[TYPE_REF]{XX10, , LXX10;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX12[TYPE_REF]{XX12, , LXX12;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX20[TYPE_REF]{XX20, , LXX20;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX21[TYPE_REF]{XX21, , LXX21;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX22[TYPE_REF]{XX22, , LXX22;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
			"XX11[TYPE_REF]{XX11, , LXX11;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}

public void testCompletionCatchArgumentName() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionCatchArgumentName.java",
		"public class CompletionCatchArgumentName {\n"+
		"	public void foo(){\n"+
		"		try{\n"+
		"			\n"+
		"		} catch (Exception ex)\n"+
		"	}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"exception[VARIABLE_DECLARATION]{exception, null, Ljava.lang.Exception;, exception, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}

public void testCompletionCatchArgumentName2() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();

	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_ARGUMENT_PREFIXES);
	options.put(JavaCore.CODEASSIST_ARGUMENT_PREFIXES,"arg"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"loc"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionCatchArgumentName2.java");

		String str = cu.getSource();
		String completeBehind = "Exception ";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:exception    completion:exception    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:locException    completion:locException    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX+ R_NON_RESTRICTED),
			requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_ARGUMENT_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}

public void testCompletionClassLiteralAfterAnonymousType1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionClassLiteralAfterAnonymousType1.java");

	String str = cu.getSource();
	String completeBehind = "double.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionConditionalExpression1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionConditionalExpression1.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var4    completion:var4    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionConditionalExpression2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionConditionalExpression2.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var4    completion:var4    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionConditionalExpression3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionConditionalExpression3.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var4    completion:var4    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}


/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=24939
*/
public void testCompletionConstructorForAnonymousType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionConstructorForAnonymousType.java");

	String str = cu.getSource();
	String completeBehind = "TypeWithConstructor(";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:TypeWithConstructor    completion:    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionEmptyToken1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionEmptyToken1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	// completion is just at start of 'zz'
	int cursorLocation = str.lastIndexOf(completeBehind);
	int start = cursorLocation;
	int end = start + 4;
	cu.codeComplete(cursorLocation, requestor);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertEquals(
			"element:clone    completion:clone()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:equals    completion:equals()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:finalize()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:foo    completion:foo()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:getClass    completion:getClass()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:hashCode    completion:hashCode()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:notify    completion:notify()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:notifyAll    completion:notifyAll()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:toString    completion:toString()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:zzyy    completion:zzyy    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResultsWithPosition());
	} else {
		assertEquals(
			"element:CompletionEmptyToken1    completion:CompletionEmptyToken1    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:clone    completion:clone()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:equals    completion:equals()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:finalize()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:foo    completion:foo()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:getClass    completion:getClass()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:hashCode    completion:hashCode()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:notify    completion:notify()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:notifyAll    completion:notifyAll()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:toString    completion:toString()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:wait    completion:wait()    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:zzyy    completion:zzyy    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResultsWithPosition());
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=100808
public void testCompletionEmptyToken2() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/testCompletionEmptyToken2/Test.java",
            "package testCompletionEmptyToken2.");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true);

    String str = this.wc.getSource();
    String completeBehind = "testCompletionEmptyToken2.";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    int start = str.lastIndexOf(completeBehind);
    int end = start + completeBehind.length();

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

	assertResults(
            "testCompletionEmptyToken2[PACKAGE_REF]{testCompletionEmptyToken2, testCompletionEmptyToken2, null, null, null, ["+start+", "+end+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}

/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25221
*/
public void testCompletionEmptyTypeName1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionEmptyTypeName1.java",
            "public class CompletionEmptyTypeName1 {\n"+
           "	void foo() {\n"+
           "		A a = new \n"+
           "	}\n"+
           "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "new ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
			"A[TYPE_REF]{A, , LA;, null, null, " +(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} else {
		assertResults(
			"CompletionEmptyTypeName1[TYPE_REF]{CompletionEmptyTypeName1, , LCompletionEmptyTypeName1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}\n"+
			"A[TYPE_REF]{A, , LA;, null, null, " +(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
	}
}

/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25221
*/
public void testCompletionEmptyTypeName2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionEmptyTypeName2.java");

	String str = cu.getSource();
	String completeBehind = " = ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertEquals(
			"element:clone    completion:clone()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:equals    completion:equals()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:getClass    completion:getClass()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:hashCode    completion:hashCode()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:notify    completion:notify()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:notifyAll    completion:notifyAll()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:toString    completion:toString()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED),
			requestor.getResults());
	} else {
		assertEquals(
			"element:A    completion:A    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:CompletionEmptyTypeName2    completion:CompletionEmptyTypeName2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:clone    completion:clone()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:equals    completion:equals()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:getClass    completion:getClass()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:hashCode    completion:hashCode()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:notify    completion:notify()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:notifyAll    completion:notifyAll()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:toString    completion:toString()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID+ R_NON_RESTRICTED),
			requestor.getResults());
	}
}

/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=41643
*/
public void testCompletionEmptyTypeName3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionEmptyTypeName3.java");

	String str = cu.getSource();
	String completeBehind = " = ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertEquals(
			"element:clone    completion:clone()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:equals    completion:equals()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:getClass    completion:getClass()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:hashCode    completion:hashCode()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:notify    completion:notify()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:notifyAll    completion:notifyAll()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:toString    completion:toString()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
	} else {
		assertEquals(
			"element:CompletionEmptyTypeName2    completion:CompletionEmptyTypeName2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:CompletionEmptyTypeName3    completion:CompletionEmptyTypeName3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:CompletionEmptyTypeName3.CompletionEmptyTypeName3_1    completion:CompletionEmptyTypeName3_1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:CompletionEmptyTypeName3_2    completion:CompletionEmptyTypeName3_2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:clone    completion:clone()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:equals    completion:equals()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:getClass    completion:getClass()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:hashCode    completion:hashCode()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:notify    completion:notify()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:notifyAll    completion:notifyAll()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:toString    completion:toString()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
			"element:wait    completion:wait()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED),
			requestor.getResults());
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=102031
public void testCompletionEmptyTypeName4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
 		"    new A().call(new /*content assist here*/)\n" +
		"  }\n" +
		"}\n" +
		"interface I {\n" +
		"  void call(TestRunnable r);\n" +
		"}\n" +
		"class A implements I{\n" +
		"  public void call(TestRunnable r) {}\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestRunnable.java",
		"package test;"+
		"public class TestRunnable {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "(new ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"A[TYPE_REF]{A, test, Ltest.A;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"I[TYPE_REF]{I, test, Ltest.I;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE +R_UNQUALIFIED +  R_NON_RESTRICTED) + "}\n" +
			"TestRunnable[TYPE_REF]{TestRunnable, test, Ltest.TestRunnable;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
/**
 * Complete at end of file.
 */
public void testCompletionEndOfCompilationUnit() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu = getCompilationUnit("Completion", "src", "", "CompletionEndOfCompilationUnit.java");
	cu.codeComplete(cu.getSourceRange().getOffset() + cu.getSourceRange().getLength(), requestor);
	assertEquals(
		"should have two methods of 'foo'",
		"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}
/*
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=66570
 */
public void testCompletionExactNameCaseInsensitive() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionExactNameCaseInsensitive.java");

	String str = cu.getSource();
	String completeBehind = "(compleTionexactnamecaseInsensitive";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:CompletionExactNameCaseInsensitive    completion:CompletionExactNameCaseInsensitive    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+ "\n" +
			"element:CompletionExactNameCaseInsensitivePlus    completion:CompletionExactNameCaseInsensitivePlus    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25820
*/
public void testCompletionExpectedTypeIsNotValid() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionExpectedTypeIsNotValid.java");

	String str = cu.getSource();
	String completeBehind = "new U";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"",
		requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95505
public void testCompletionExpectedTypeOnEmptyToken1() throws JavaModelException {
	ICompilationUnit aType = null;
	try {

		aType = getWorkingCopy(
	            "/Completion/src/test/AType.java",
	            "package test;\n" +
	            "public class AType{\n"+
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/Test.java",
	            "package test;\n" +
	            "public class Test{\n"+
	            "  void foo() {\n"+
	            "    AType a = new \n"+
	            "  }\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "AType a = new ";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
	            "expectedTypesSignatures={Ltest.AType;}\n"+
	            "expectedTypesKeys={Ltest/AType;}",
	            requestor.getContext());
	    if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
			assertResults(
		            "AType[TYPE_REF]{AType, test, Ltest.AType;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		            requestor.getResults());
	    } else {
	    	assertResults(
		            "Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
					"AType[TYPE_REF]{AType, test, Ltest.AType;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		            requestor.getResults());
	    }
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}


// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95505
public void testCompletionExpectedTypeOnEmptyToken3() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/test/AType.java",
	            "package test;\n" +
	            "public class AType{\n"+
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/Test.java",
	            "package test;\n" +
	            "public class Test{\n"+
	            "  void foo() {\n"+
	            "    AType a = \n"+
	            "  }\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    requestor.setIgnored(CompletionProposal.METHOD_REF, true);
	    requestor.setIgnored(CompletionProposal.FIELD_REF, true);
	    requestor.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);

	    String str = this.wc.getSource();
	    String completeBehind = "AType a = ";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
	            "expectedTypesSignatures={Ltest.AType;}\n"+
	            "expectedTypesKeys={Ltest/AType;}",
	            requestor.getContext());
	    if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
			assertResults(
		            "",
		            requestor.getResults());
	    } else {
	    	assertResults(
		            "Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
					"AType[TYPE_REF]{AType, test, Ltest.AType;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		            requestor.getResults());
	    }
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}


// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95505
public void testCompletionExpectedTypeOnEmptyToken4() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/test/AInterface.java",
	            "package test;\n" +
	            "public interface AInterface{\n"+
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/Test.java",
	            "package test;\n" +
	            "public class Test{\n"+
	            "  void foo() {\n"+
	            "    AInterface a = new \n"+
	            "  }\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	    String str = this.wc.getSource();
	    String completeBehind = "AInterface a = new ";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
	            "expectedTypesSignatures={Ltest.AInterface;}\n"+
	            "expectedTypesKeys={Ltest/AInterface;}",
	            requestor.getContext());

	    if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
			assertResults(
		            "AInterface[TYPE_REF]{AInterface, test, Ltest.AInterface;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		            requestor.getResults());
	    } else {
	    	assertResults(
		            "Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
					"AInterface[TYPE_REF]{AInterface, test, Ltest.AInterface;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
		            requestor.getResults());
	    }
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}


public void testCompletionFieldInitializer1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFieldInitializer1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionFieldInitializer2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFieldInitializer2.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionFieldInitializer3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFieldInitializer3.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionFieldInitializer4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFieldInitializer4.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}


public void testCompletionFieldName() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFieldName.java");

	String str = cu.getSource();
	String completeBehind = "ClassWithComplexName ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:classWithComplexName    completion:classWithComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:complexName2    completion:complexName2    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:name    completion:name    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:withComplexName    completion:withComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
		requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testCompletionFieldName2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  TypeNameRequestor name\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TypeNameRequestor name";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"nameTypeNameRequestor[VARIABLE_DECLARATION]{nameTypeNameRequestor, null, Ltest.TypeNameRequestor;, nameTypeNameRequestor, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}\n" +
			"nameRequestor[VARIABLE_DECLARATION]{nameRequestor, null, Ltest.TypeNameRequestor;, nameRequestor, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=38111
public void testCompletionFieldName3() throws JavaModelException { 
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  public static final TypeNameRequestor \n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "final TypeNameRequestor ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"NAME_REQUESTOR[VARIABLE_DECLARATION]{NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}\n" +
			"REQUESTOR[VARIABLE_DECLARATION]{REQUESTOR, null, Ltest.TypeNameRequestor;, REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}\n" +
			"TYPE_NAME_REQUESTOR[VARIABLE_DECLARATION]{TYPE_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, TYPE_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=38111
public void testCompletionFieldName4() throws JavaModelException { 
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  public static final TypeNameRequestor nam\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "final TypeNameRequestor nam";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"NAM_REQUESTOR[VARIABLE_DECLARATION]{NAM_REQUESTOR, null, Ltest.TypeNameRequestor;, NAM_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"NAM_TYPE_NAME_REQUESTOR[VARIABLE_DECLARATION]{NAM_TYPE_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, NAM_TYPE_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"NAME_REQUESTOR[VARIABLE_DECLARATION]{NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_NAME_LESS_NEW_CHARACTERS+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=38111
public void testCompletionFieldName5() throws JavaModelException { 
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  public static final TypeNameRequestor ZZ\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "final TypeNameRequestor ZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZ_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZ_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZ_REQUESTOR[VARIABLE_DECLARATION]{ZZ_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZ_TYPE_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZ_TYPE_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_TYPE_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=38111
public void testCompletionFieldName6() throws JavaModelException { 
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  public static final TypeNameRequestor ZZN\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "final TypeNameRequestor ZZN";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZN_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZN_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZN_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZN_REQUESTOR[VARIABLE_DECLARATION]{ZZN_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZN_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZN_TYPE_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZN_TYPE_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZN_TYPE_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=38111
public void testCompletionFieldName7() throws JavaModelException { 
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TypeNameRequestor.java",
		"package test;"+
		"public class TypeNameRequestor {\n" +
		"  public static final TypeNameRequestor ZZ_N\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "final TypeNameRequestor ZZ_N";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZ_N_REQUESTOR[VARIABLE_DECLARATION]{ZZ_N_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_N_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZ_N_TYPE_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZ_N_TYPE_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_N_TYPE_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZ_NAME_REQUESTOR[VARIABLE_DECLARATION]{ZZ_NAME_REQUESTOR, null, Ltest.TypeNameRequestor;, ZZ_NAME_REQUESTOR, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

/**
 * Complete the type "A" from "new A".
 */
public void testCompletionFindClass() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindClass.java",
            "public class CompletionFindClass {\n" +
            "	private    A[] a;\n" +
            "	public CompletionFindClass () {\n" +
            "		this.a = new A\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "A";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"ABC[TYPE_REF]{p1.ABC, p1, Lp1.ABC;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
    		"ABC[TYPE_REF]{p2.ABC, p2, Lp2.ABC;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
			"A3[TYPE_REF]{A3, , LA3;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"A[TYPE_REF]{A, , LA;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
            requestor.getResults());
}


/**
 * The same type must be find only once
 */
public void testCompletionFindClass2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindClass2.java");

	String str = cu.getSource();
	String completeBehind = "PX";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one classe",
		"element:PX    completion:pack1.PX    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_QUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}


/**
 * Complete the type "Default" in the default package example.
 */
public void testCompletionFindClassDefaultPackage() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionDefaultPackage.java");

	String str = cu.getSource();
	String completeBehind = "Def";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one class",
		"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}


/**
 * Complete the constructor "CompletionFindConstructor" from "new CompletionFindConstructor(".
 */
public void testCompletionFindConstructor() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindConstructor.java",
            "public class CompletionFindConstructor {\n"+
            "	public CompletionFindConstructor (int i) {\n"+
            "	}\n"+
            "	publuc void foo(){\n"+
            "		int x = 45;\n"+
            "		new CompletionFindConstructor(i);\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "CompletionFindConstructor(";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

   assertResults(
			"CompletionFindConstructor[ANONYMOUS_CLASS_DECLARATION]{, LCompletionFindConstructor;, (I)V, null, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"CompletionFindConstructor[METHOD_REF<CONSTRUCTOR>]{, LCompletionFindConstructor;, (I)V, CompletionFindConstructor, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}


/**
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=78801
 */
public void testCompletionFindConstructor2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindConstructor2.java",
            "import zconstructors.*;\n"+
            "public class CompletionFindConstructor2 {\n"+
            "	Constructor2 c = new Constructor2();\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "Constructor2(";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

    assertEquals(
			"Constructor2[ANONYMOUS_CLASS_DECLARATION]{, Lzconstructors.Constructor2;, ()V, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Constructor2[METHOD_REF<CONSTRUCTOR>]{, Lzconstructors.Constructor2;, ()V, Constructor2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

/**
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=78801
 */
public void testCompletionFindConstructor3() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindConstructor3.java",
            "import zconstructors.*;\n"+
            "public class CompletionFindConstructor3 {\n"+
            "	Constructor3 c = new Constructor3();\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "Constructor3(";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

    assertEquals(
			"Constructor3[ANONYMOUS_CLASS_DECLARATION]{, Lzconstructors.Constructor3;, ()V, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Constructor3[METHOD_REF<CONSTRUCTOR>]{, Lzconstructors.Constructor3;, ()V, Constructor3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

/**
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=78801
 */
public void testCompletionFindConstructor4() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindConstructor4.java",
            "import zconstructors.*;\n"+
            "public class CompletionFindConstructor4 {\n"+
            "	Constructor4 c = new Constructor4();\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "Constructor4(";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

	assertEquals(
			"Constructor4[ANONYMOUS_CLASS_DECLARATION]{, Lzconstructors.Constructor4;, (I)V, null, (i), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Constructor4[METHOD_REF<CONSTRUCTOR>]{, Lzconstructors.Constructor4;, (I)V, Constructor4, (i), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

/**
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=78801
 */
public void testCompletionFindConstructor5() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindConstructor5.java",
            "import zconstructors.*;\n"+
            "public class CompletionFindConstructor5 {\n"+
            "	Constructor5 c = new Constructor5();\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "Constructor5(";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

	assertEquals(
			"Constructor5[ANONYMOUS_CLASS_DECLARATION]{, Lzconstructors.Constructor5;, (I)V, null, (i), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"Constructor5[METHOD_REF<CONSTRUCTOR>]{, Lzconstructors.Constructor5;, (I)V, Constructor5, (i), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

/**
 * Complete the exception "Exception" in a catch clause.
 */
public void testCompletionFindExceptions1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindException1.java");

	String str = cu.getSource();
	String completeBehind = "Ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one class",
		"element:Exception    completion:Exception    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}

/**
 * Complete the exception "Exception" in a throws clause.
 */
public void testCompletionFindExceptions2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindException2.java");

	String str = cu.getSource();
	String completeBehind = "Ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one class",
		"element:Exception    completion:Exception    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}

/**
 * Complete the field "var" from "va";
 */
public void testCompletionFindField1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindField1.java");

	String str = cu.getSource();
	String completeBehind = "va";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one field: 'var' and one variable: 'var'",
		"element:var    completion:this.var    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n"+
		"element:var    completion:var    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}

/**
 * Complete the field "var" from "this.va";
 */
public void testCompletionFindField2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindField2.java");

	String str = cu.getSource();
	String completeBehind = "va";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have 1 field of starting with 'va'",
		"element:var    completion:var    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionFindField3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindField3.java");

	String str = cu.getSource();
	String completeBehind = "b.ba";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:bar    completion:bar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED),
		requestor.getResults());
}

/**
 * Complete the import, "import pac"
 */
public void testCompletionFindImport1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindImport1.java",
            "import pac\n"+
            "\n"+
            "public class CompletionFindImport1 {\n"+
            "\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "pac";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"pack[PACKAGE_REF]{pack.*;, pack, null, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"pack1[PACKAGE_REF]{pack1.*;, pack1, null, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"pack1.pack3[PACKAGE_REF]{pack1.pack3.*;, pack1.pack3, null, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"pack2[PACKAGE_REF]{pack2.*;, pack2, null, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) +"}",
			requestor.getResults());
}

public void testCompletionFindImport2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindImport2.java",
            "import pack1.P\n"+
            "\n"+
            "public class CompletionFindImport2 {\n"+
            "\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "pack1.P";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"pack1.pack3[PACKAGE_REF]{pack1.pack3.*;, pack1.pack3, null, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n"+
			"PX[TYPE_REF]{PX;, pack1, Lpack1.PX;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}

/**
 * Complete the local variable "var";
 */
public void testCompletionFindLocalVariable() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindLocalVariable.java");

	String str = cu.getSource();
	String completeBehind = "va";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have one local variable of 'var'",
		"element:var    completion:var    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionFindMemberType1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindMemberType1.java",
            "interface A1 {\n"+
            "	class Inner1 {\n"+
            "	}\n"+
            "}\n"+
            "interface B1 extends A1 {\n"+
            "	class Inner1 {\n"+
            "	}\n"+
            "}\n"+
            "public class CompletionFindMemberType1 {\n"+
            "	public void foo() {\n"+
            "		B1.Inner\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "Inner";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"B1.Inner1[TYPE_REF]{Inner1, , LB1$Inner1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) +"}",
		requestor.getResults());
}

public void testCompletionFindMemberType2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixMethodName2.java",
            "interface A2 {\n"+
            "	class ZInner2{\n"+
            "	}\n"+
            "}\n"+
            "interface B2 extends A2 {\n"+
            "	class ZInner2 {\n"+
            "	}\n"+
            "}\n"+
            "public class CompletionFindMemberType2 implements B2{\n"+
            "	public void foo() {\n"+
            "		ZInner\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "ZInner";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"B2.ZInner2[TYPE_REF]{ZInner2, , LB2$ZInner2;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
		requestor.getResults());
}

/**
 * Complete the method call "a.foobar" from "a.fooba";
 */
public void testCompletionFindMethod1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindMethod1.java");

	String str = cu.getSource();
	String completeBehind = "fooba";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have two methods of 'foobar'",
		"element:foobar    completion:foobar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"\n" +
		"element:foobar    completion:foobar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED),
		requestor.getResults());
}
/**
 * Too much Completion match on interface
 */
public void testCompletionFindMethod2() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindMethod2.java");

	String str = cu.getSource();
	String completeBehind = "fooba";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:foobar    completion:foobar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"\n" +
		"element:foobar    completion:foobar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED),
		requestor.getResults());
}
/**
 * Complete the method call "foobar" from "fooba";
 */
public void testCompletionFindMethodInThis() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindMethodInThis.java");

	String str = cu.getSource();
	String completeBehind = "fooba";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have one method of 'foobar'",
		"element:foobar    completion:foobar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
}

/**
 * Complete the method call "foobar" from "fooba".  The compilation
 * unit simulates typing in process; ie it has incomplete structure/syntax errors.
 */
public void testCompletionFindMethodWhenInProcess() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindMethodInProcess.java");

	String str = cu.getSource();
	String completeBehind = "fooba";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have a method of 'foobar'",
		"element:foobar    completion:foobar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
		requestor.getResults());
	cu.close();
}

public void testCompletionFindSecondaryType1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindSecondaryType1.java");

	String str = cu.getSource();
	String completeBehind = "/**/Secondary";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:SecondaryType1    completion:SecondaryType1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:SecondaryType2    completion:SecondaryType2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionFindSuperInterface() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionFindSuperInterface.java",
            "public class CompletionFindSuperInterface implements SuperInterface {\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "Super";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
           "SuperInterface[TYPE_REF]{SuperInterface, , LSuperInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}

/**
 * Complete the field "bar" from "this.ba"
 */
public void testCompletionFindThisDotField() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindThisDotField.java");

	String str = cu.getSource();
	String completeBehind = "this.ba";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have one result of 'bar'",
		"element:bar    completion:bar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED),
		requestor.getResults());
}

public void testCompletionImportedType1() throws JavaModelException {
    this.workingCopies = new ICompilationUnit[2];
    this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/imported/ZZZZ.java",
		"package test.imported;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/CompletionImportedType1.java",
			"package test;"+
			"public class CompletionImportedType1 {"+
			"  ZZZ\n"+
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[1].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[1].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, Ltest.CompletionImportedType1;, ()V, ZZZ, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported.ZZZZ, test.imported, Ltest.imported.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionImportedType2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/imported1/ZZZZ.java",
		"package test.imported1;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/imported2/ZZZZ.java",
		"package test.imported2;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/test/imported3/ZZZZ.java",
		"package test.imported3;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");

	this.workingCopies[3] = getWorkingCopy(
		"/Completion/src/test/CompletionImportedType2.java",
		"package test;"+
		"import test.imported1.*;"+
		"import test.imported2.*;"+
		"import test.imported3.*;"+
		"public class CompletionImportedType2 {"+
		"  ZZZ\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[3].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[3].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, Ltest.CompletionImportedType2;, ()V, ZZZ, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported1.ZZZZ, test.imported1, Ltest.imported1.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported2.ZZZZ, test.imported2, Ltest.imported2.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported3.ZZZZ, test.imported3, Ltest.imported3.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionImportedType3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[4];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/imported1/ZZZZ.java",
		"package test.imported1;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/imported2/ZZZZ.java",
		"package test.imported2;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/test/imported3/ZZZZ.java",
		"package test.imported3;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");

	this.workingCopies[3] = getWorkingCopy(
		"/Completion/src/test/CompletionImportedType3.java",
		"package test;"+
		"import test.imported2.*;"+
		"public class CompletionImportedType3 {"+
		"  ZZZ\n"+
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[3].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[3].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, Ltest.CompletionImportedType3;, ()V, ZZZ, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported1.ZZZZ, test.imported1, Ltest.imported1.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported3.ZZZZ, test.imported3, Ltest.imported3.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{ZZZZ, test.imported2, Ltest.imported2.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionImportedType4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/imported1/ZZZZ.java",
		"package test.imported1;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/imported2/ZZZZ.java",
		"package test.imported2;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");

	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/test/CompletionImportedType4.java",
		"package test;"+
		"import test.imported1.*;"+
		"public class CompletionImportedType4 {"+
		"  ZZZ\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[2].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[2].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, Ltest.CompletionImportedType4;, ()V, ZZZ, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported2.ZZZZ, test.imported2, Ltest.imported2.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{ZZZZ, test.imported1, Ltest.imported1.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionImportedType5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/imported1/ZZZZ.java",
		"package test.imported1;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/imported2/ZZZZ.java",
		"package test.imported2;"+
		"public class ZZZZ {\n"+
		"  \n"+
		"}");

	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/test/CompletionImportedType5.java",
		"package test;"+
		"import test.imported2.*;"+
		"public class CompletionImportedType5 {"+
		"  ZZZ\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[2].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[2].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, Ltest.CompletionImportedType5;, ()V, ZZZ, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{test.imported1.ZZZZ, test.imported1, Ltest.imported1.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"ZZZZ[TYPE_REF]{ZZZZ, test.imported2, Ltest.imported2.ZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=171037
public void testCompletionImportedType6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"import AClas;"+
		"public class Test {\n"+
		"  \n"+
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/AClass1.java",
		"public class AClass1 {"+
		"}");

	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/test/p/AClass2.java",
		"package test.p;"+
		"public class AClass2 {"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "AClas";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"AClass2[TYPE_REF]{test.p.AClass2;, test.p, Ltest.p.AClass2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=243406
public void testCompletionImportedType7() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"import AClas.Foo;"+
		"public class Test {\n"+
		"  \n"+
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test2/AClass1.java",
		"package test2;"+
		"public class AClass {"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "AClas";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int start = str.indexOf("AClas");
	int end = start + "AClas".length();
	assertResults(
			"AClass[TYPE_REF]{test2.AClass;, test2, Ltest2.AClass;, null, null, ["+start+", "+end+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=243406
public void testCompletionImportedType8() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"import test2.AClas.Foo;"+
		"public class Test {\n"+
		"  \n"+
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test2/AClass1.java",
		"package test2;"+
		"public class AClass {"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "AClas";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int start = str.indexOf("AClas");
	int end = start + "AClas".length();
	assertResults(
			"AClass[TYPE_REF]{AClass;, test2, Ltest2.AClass;, null, null, ["+start+", "+end+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends1() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends1.java",
			"package test;\n" +
			"public class CompletionInsideExtends1 extends  {\n" +
			"  public class CompletionInsideExtends1Inner {}\n" +
			"}\n" +
			"class CompletionInsideExtends1TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"CompletionInsideExtends1TopLevel[TYPE_REF]{CompletionInsideExtends1TopLevel, test, Ltest.CompletionInsideExtends1TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}

}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends10() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends10.java",
			"package test;\n" +
			"public interface CompletionInsideExtends10 {\n" +
			"  public interface CompletionInsideExtends10Inner extends CompletionInsideExtends{\n" +
			"    public interface CompletionInsideExtends10InnerInner {\n" +
			"    }\n" +
			"  }\n" +
			"}\n" +
			"interface CompletionInsideExtends10TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideExtends10[TYPE_REF]{CompletionInsideExtends10, test, Ltest.CompletionInsideExtends10;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionInsideExtends10TopLevel[TYPE_REF]{CompletionInsideExtends10TopLevel, test, Ltest.CompletionInsideExtends10TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends11() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends11.java",
			"package test;\n" +
			"public class CompletionInsideExtends11 implements {\n" +
			"  public class CompletionInsideExtends11Inner {\n" +
			"  }\n" +
			"}\n" +
			"class CompletionInsideExtends11TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "implements ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"",
				requestor.getResults());
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends12() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends12.java",
			"package test;\n" +
			"public class CompletionInsideExtends12 implements CompletionInsideExtends {\n" +
			"  public class CompletionInsideExtends12Inner {\n" +
			"  }\n" +
			"}\n" +
			"class CompletionInsideExtends12TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "implements CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=212713
public void testCompletionInsideExtends13() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n"+
		"public class Test {\n"+
		"        static class GrrrBug {}\n"+
		"        class Foo extends Grrr\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Grrr";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"Test.GrrrBug[TYPE_REF]{GrrrBug, test, Ltest.Test$GrrrBug;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=212713
public void testCompletionInsideExtends14() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/GrrrBug1.java",
		"package test;\n"+
		"public class GrrrBug1 {\n"+
		"	public static class GrrrBug2 {\n"+
		"		public static class GrrrBug3 {}\n"+
 		"       }\n"+
		"	public static class GrrrBug4 extends Grrr {\n"+
		"		public static class GrrrBug5 {}\n"+
		"    }\n"+
		"}\n"+
		"class GrrrBug6 {\n"+
		"	public static class GrrrBug7 {}\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "extends Grrr";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"GrrrBug1.GrrrBug2.GrrrBug3[TYPE_REF]{test.GrrrBug1.GrrrBug2.GrrrBug3, test, Ltest.GrrrBug1$GrrrBug2$GrrrBug3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"GrrrBug6.GrrrBug7[TYPE_REF]{test.GrrrBug6.GrrrBug7, test, Ltest.GrrrBug6$GrrrBug7;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"GrrrBug1[TYPE_REF]{GrrrBug1, test, Ltest.GrrrBug1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"GrrrBug1.GrrrBug2[TYPE_REF]{GrrrBug2, test, Ltest.GrrrBug1$GrrrBug2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"GrrrBug6[TYPE_REF]{GrrrBug6, test, Ltest.GrrrBug6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends2() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends2.java",
			"package test;\n" +
			"public class CompletionInsideExtends2 extends CompletionInsideExtends {\n" +
			"  public class CompletionInsideExtends2Inner {}\n" +
			"}\n" +
			"class CompletionInsideExtends2TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideExtends2TopLevel[TYPE_REF]{CompletionInsideExtends2TopLevel, test, Ltest.CompletionInsideExtends2TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends3() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends3.java",
			"package test;\n" +
			"public class CompletionInsideExtends3 {\n" +
			"  public class CompletionInsideExtends3Inner extends {\n" +
			"    public class CompletionInsideExtends3InnerInner {\n" +
			"    }\n" +
			"  }\n" +
			"}\n" +
			"class CompletionInsideExtends3TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"CompletionInsideExtends3[TYPE_REF]{CompletionInsideExtends3, test, Ltest.CompletionInsideExtends3;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionInsideExtends3TopLevel[TYPE_REF]{CompletionInsideExtends3TopLevel, test, Ltest.CompletionInsideExtends3TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends4() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends4.java",
			"package test;\n" +
			"public class CompletionInsideExtends4 {\n" +
			"  public class CompletionInsideExtends4Inner extends CompletionInsideExtends{\n" +
			"    public class CompletionInsideExtends4InnerInner {\n" +
			"    }\n" +
			"  }\n" +
			"\n}" +
			"class CompletionInsideExtends4TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideExtends4[TYPE_REF]{CompletionInsideExtends4, test, Ltest.CompletionInsideExtends4;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionInsideExtends4TopLevel[TYPE_REF]{CompletionInsideExtends4TopLevel, test, Ltest.CompletionInsideExtends4TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends5() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends5.java",
			"package test;\n" +
			"public class CompletionInsideExtends5 {\n" +
			"  void foo() {\n" +
			"    public class CompletionInsideExtends5Inner extends {\n" +
			"      public class CompletionInsideExtends5InnerInner {\n" +
			"      }\n" +
			"    }\n" +
			"  }\n" +
			"}\n" +
			"class CompletionInsideExtends5TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"CompletionInsideExtends5[TYPE_REF]{CompletionInsideExtends5, test, Ltest.CompletionInsideExtends5;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionInsideExtends5TopLevel[TYPE_REF]{CompletionInsideExtends5TopLevel, test, Ltest.CompletionInsideExtends5TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends6() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends6.java",
			"package test;\n" +
			"public class CompletionInsideExtends6 {\n" +
			"  void foo() {\n" +
			"    public class CompletionInsideExtends6Inner extends CompletionInsideExtends {\n" +
			"      public class CompletionInsideExtends6InnerInner {\n" +
			"      }\n" +
			"    }\n" +
			"  }\n" +
			"}\n" +
			"class CompletionInsideExtends6TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideExtends6[TYPE_REF]{CompletionInsideExtends6, test, Ltest.CompletionInsideExtends6;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"CompletionInsideExtends6TopLevel[TYPE_REF]{CompletionInsideExtends6TopLevel, test, Ltest.CompletionInsideExtends6TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends7() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends7.java",
			"package test;\n" +
			"public interface CompletionInsideExtends7 extends  {\n" +
			"  public interface CompletionInsideExtends7Inner {}\n" +
			"}\n" +
			"interface CompletionInsideExtends7TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"CompletionInsideExtends7TopLevel[TYPE_REF]{CompletionInsideExtends7TopLevel, test, Ltest.CompletionInsideExtends7TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends8() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends8.java",
			"package test;\n" +
			"public interface CompletionInsideExtends8 extends CompletionInsideExtends {\n" +
			"  public interface CompletionInsideExtends8Inner {}\n" +
			"}\n" +
			"interface CompletionInsideExtends8TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends CompletionInsideExtends";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideExtends8TopLevel[TYPE_REF]{CompletionInsideExtends8TopLevel, test, Ltest.CompletionInsideExtends8TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=78151
public void testCompletionInsideExtends9() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideExtends9.java",
			"package test;\n" +
			"public interface CompletionInsideExtends9 {\n" +
			"  public interface CompletionInsideExtends9Inner extends {\n" +
			"    public interface CompletionInsideExtends9InnerInner {\n" +
			"    }\n" +
			"  }\n" +
			"}\n" +
			"interface CompletionInsideExtends9TopLevel {\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"",
				requestor.getResults());
	} else {
		assertResults(
				"CompletionInsideExtends9[TYPE_REF]{CompletionInsideExtends9, test, Ltest.CompletionInsideExtends9;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"CompletionInsideExtends9TopLevel[TYPE_REF]{CompletionInsideExtends9TopLevel, test, Ltest.CompletionInsideExtends9TopLevel;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=82740
public void testCompletionInsideGenericClass() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionInsideGenericClass.java",
			"package test;\n" +
			"public class CompletionInsideGenericClass <CompletionInsideGenericClassParameter> {\n" +
			"  CompletionInsideGenericClas\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "CompletionInsideGenericClas";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionInsideGenericClas[POTENTIAL_METHOD_DECLARATION]{CompletionInsideGenericClas, Ltest.CompletionInsideGenericClass;, ()V, CompletionInsideGenericClas, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"CompletionInsideGenericClass[TYPE_REF]{CompletionInsideGenericClass, test, Ltest.CompletionInsideGenericClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void testCompletionInsideStaticMethod() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionInsideStaticMethod.java");

	String str = cu.getSource();
	String completeBehind = "doT";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:doTheThing    completion:doTheThing()    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionInstanceofOperator1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionInstanceofOperator1.java");

		String str = cu.getSource();
		String completeBehind = "x instanceof WWWCompletionInstanceof";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:WWWCompletionInstanceof1    completion:WWWCompletionInstanceof1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:WWWCompletionInstanceof2    completion:WWWCompletionInstanceof2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:WWWCompletionInstanceof3    completion:WWWCompletionInstanceof3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:WWWCompletionInstanceof4    completion:WWWCompletionInstanceof4    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionKeywordAbstract1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract1.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionKeywordAbstract10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract10.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}

public void testCompletionKeywordAbstract11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract11.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract12.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionKeywordAbstract13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract13.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}

public void testCompletionKeywordAbstract14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract14.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}

public void testCompletionKeywordAbstract15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract15.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract16.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract2.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordAbstract3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract3.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract4.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract5.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract6.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordAbstract7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract7.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract8.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAbstract9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAbstract9.java");

		String str = cu.getSource();
		String completeBehind = "abs";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:abstract    completion:abstract    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAssert1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert1.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:assert    completion:assert    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAssert2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert2.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordAssert3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert3.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordAssert4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert4.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:assert    completion:assert    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordAssert5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert5.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordAssert6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordAssert6.java");

		String str = cu.getSource();
		String completeBehind = "as";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordBreak1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak1.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:break    completion:break    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordBreak2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak2.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordBreak3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak3.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:break    completion:break    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordBreak4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak4.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:break    completion:break    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordBreak5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak5.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordBreak6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordBreak6.java");

		String str = cu.getSource();
		String completeBehind = "bre";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:break    completion:break    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase1.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase10.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCase2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase2.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase3.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase4.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase5.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCase6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase6.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase7.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase8.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCase9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCase9.java");

		String str = cu.getSource();
		String completeBehind = "cas";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:case    completion:case    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCatch1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch1.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:catch    completion:catch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCatch10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch10.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:catch    completion:catch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:catchz    completion:catchz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCatch2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch2.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCatch3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch3.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCatch4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch4.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCatch5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch5.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:catch    completion:catch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:catchz    completion:catchz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCatch6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch6.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:catch    completion:catch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordCatch7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch7.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCatch8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch8.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordCatch9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordCatch9.java");

		String str = cu.getSource();
		String completeBehind = "cat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordClass1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass1.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass10.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass11.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass12.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass13.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass14.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass15.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass16.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass17() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass17.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass18() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass18.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass19() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass19.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass2.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass20() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass20.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass21() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass21.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass22() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass22.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass23() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass23.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass24() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass24.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass3.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass4.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass5.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass6.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass7.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass8.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordClass9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordClass9.java");

		String str = cu.getSource();
		String completeBehind = "cla";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Class    completion:Class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:ClassWithComplexName    completion:ClassWithComplexName    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:class    completion:class    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordContinue1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordContinue1.java",
		"public class CompletionKeywordContinue1 {\n" +
		"	void foo() {\n" +
		"		for(;;) {\n" +
		"			{\n" +
		"				cont\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cont";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"continue[KEYWORD]{continue, null, null, continue, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordContinue2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordContinue2.java",
		"public class CompletionKeywordContinue2 {\n" +
		"	void foo() {\n" +
		"		if(true) {\n" +
		"			cont\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cont";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
public void testCompletionKeywordContinue3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordContinue3.java",
		"public class CompletionKeywordContinue3 {\n" +
		"	void foo() {\n" +
		"		#\n" +
		"		for(;;) {\n" +
		"			{\n" +
		"				cont\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cont";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"continue[KEYWORD]{continue, null, null, continue, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordContinue4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordContinue4.java",
		"public class CompletionKeywordContinue4 {\n" +
		"	void foo() {\n" +
		"		#\n" +
		"		if(true) {\n" +
		"			cont\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cont";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=176364
public void testCompletionKeywordContinue5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
 		"    for (int i = 0; i < 1; i++) {\n" +
 		"      switch (i) {\n" +
 		"        case 0:\n" +
 		"          conti\n" +
 		"        break;\n" +
 		"      }\n" +
 		"    }\n" +
		"  }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "conti";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"continue[KEYWORD]{continue, null, null, continue, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordDefault1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault1.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault10.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault2.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault3.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault4.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault5.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault6.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault7.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault8.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:default    completion:default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDefault9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDefault9.java");

		String str = cu.getSource();
		String completeBehind = "def";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Default    completion:Default    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo1.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:do    completion:do    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"\n"+
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo2.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo3.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo4.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:do    completion:do    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"\n"+
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo5.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordDo6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordDo6.java");

		String str = cu.getSource();
		String completeBehind = "do";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:double    completion:double    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordElse1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse1.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:else    completion:else    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordElse2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse2.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordElse3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse3.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordElse4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse4.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordElse5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse5.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:else    completion:else    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordElse6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse6.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordElse7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse7.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordElse8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordElse8.java");

		String str = cu.getSource();
		String completeBehind = "els";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends1.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:extends    completion:extends    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordExtends10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends10.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends2.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends3.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends4.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:extends    completion:extends    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordExtends5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends5.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends6.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:extends    completion:extends    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordExtends7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends7.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends8.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordExtends9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordExtends9.java");

		String str = cu.getSource();
		String completeBehind = "ext";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:extends    completion:extends    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFalse1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFalse1.java");

		String str = cu.getSource();
		String completeBehind = "fal";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFalse2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFalse2.java");

		String str = cu.getSource();
		String completeBehind = "fal";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:false    completion:false    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFalse3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFalse3.java");

		String str = cu.getSource();
		String completeBehind = "fal";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFalse4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFalse4.java");

		String str = cu.getSource();
		String completeBehind = "fal";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:false    completion:false    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=95008
public void testCompletionKeywordFalse5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  boolean test = ;\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boolean test = ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"Test[TYPE_REF]{Test, test, Ltest.Test;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}\n" +
			"false[KEYWORD]{false, null, null, false, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED) + "}\n" +
			"true[KEYWORD]{true, null, null, true, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordFinal1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal1.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal10.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal11.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinal12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal12.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal13.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal14.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:protected void finalize() throws Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal15.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinal16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal16.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:protected void finalize() throws Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal17() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal17.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal18() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal18.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal2.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinal3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal3.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal4.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal5.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:protected void finalize() throws Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal6.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinal7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal7.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:protected void finalize() throws Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal8.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinal9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinal9.java");

		String str = cu.getSource();
		String completeBehind = "fin";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:final    completion:final    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finalize    completion:finalize()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE +R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally1.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally10.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinally11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally11.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinally12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally12.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally13.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finallyz    completion:finallyz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally14.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finallyz    completion:finallyz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally2.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinally3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally3.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinally4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally4.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFinally5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally5.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally6.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:finallyz    completion:finallyz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally7.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finallyz    completion:finallyz    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally8.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:finally    completion:finally    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFinally9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFinally9.java");

		String str = cu.getSource();
		String completeBehind = "finall";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFor1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor1.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:for    completion:for    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFor2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor2.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFor3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor3.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordFor4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor4.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:for    completion:for    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFor5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor5.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordFor6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordFor6.java");

		String str = cu.getSource();
		String completeBehind = "fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordIf1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf1.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:if    completion:if    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordIf2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf2.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordIf3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf3.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordIf4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf4.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:if    completion:if    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordIf5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf5.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordIf6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordIf6.java");

		String str = cu.getSource();
		String completeBehind = "if";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordImplements1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements1.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:implements    completion:implements    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImplements2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements2.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:implements    completion:implements    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImplements3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements3.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordImplements4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements4.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:implements    completion:implements    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImplements5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements5.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:implements    completion:implements    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImplements6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImplements6.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordImport1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport1.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImport2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "p", "CompletionKeywordImport2.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImport3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport3.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImport4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport4.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordImport5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport5.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImport6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport6.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordImport7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordImport7.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordImport8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "p", "CompletionKeywordImport8.java");

		String str = cu.getSource();
		String completeBehind = "imp";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:import    completion:import    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInstanceof1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof1.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:instanceof    completion:instanceof    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInstanceof2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof2.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordInstanceof3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof3.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordInstanceof4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof4.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:instanceof    completion:instanceof    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInstanceof5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof5.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordInstanceof6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInstanceof6.java");

		String str = cu.getSource();
		String completeBehind = "ins";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordInterface1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface1.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface1.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface11.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface12.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface13.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface14.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface15.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface16.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface17() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface17.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface18() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface18.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface2.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface3.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface4.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface5.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface6.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface7.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface8.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordInterface9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordInterface9.java");

		String str = cu.getSource();
		String completeBehind = "interf";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:interface    completion:interface    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNative1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative1.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:native    completion:native    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNative2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative2.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNative3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative3.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:native    completion:native    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNative4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative4.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNative5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative5.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:native    completion:native    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNative6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative6.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNative7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative7.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:native    completion:native    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNative8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNative8.java");

		String str = cu.getSource();
		String completeBehind = "nat";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNew1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew1.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew10.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew11.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew12.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew13.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew14.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew15.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew16.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew2.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew3.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew4.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew5.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew6.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew7.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew8.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNew9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNew9.java");

		String str = cu.getSource();
		String completeBehind = "ne";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:new    completion:new    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNull1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNull1.java");

		String str = cu.getSource();
		String completeBehind = "nul";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNull2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNull2.java");

		String str = cu.getSource();
		String completeBehind = "nul";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:null    completion:null    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordNull3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNull3.java");

		String str = cu.getSource();
		String completeBehind = "nul";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordNull4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordNull4.java");

		String str = cu.getSource();
		String completeBehind = "nul";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:null    completion:null    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPackage1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage1.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:package    completion:package    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPackage2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "p", "CompletionKeywordPackage2.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}

public void testCompletionKeywordPackage3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage3.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPackage4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage4.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPackage5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage5.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPackage6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage6.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPackage7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPackage7.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPackage8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "p", "CompletionKeywordPackage8.java");

		String str = cu.getSource();
		String completeBehind = "pac";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPrivate1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate1.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate10.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPrivate2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate2.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate3.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate4.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPrivate5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate5.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPrivate6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate6.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate7.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate8.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:private    completion:private    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPrivate9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPrivate9.java");

		String str = cu.getSource();
		String completeBehind = "pri";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordProtected1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected1.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected10.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordProtected2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected2.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected3.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected4.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordProtected5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected5.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordProtected6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected6.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected7.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected8.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:protected    completion:protected    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordProtected9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordProtected9.java");

		String str = cu.getSource();
		String completeBehind = "pro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic1.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic10.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic11.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic12.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic13.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic14.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic15() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic15.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic16() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic16.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic17() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic17.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic18() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic18.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic19() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic19.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic2.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic20() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic10.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic3.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic4.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic5.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordPublic6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic6.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic7.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic8.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:public    completion:public    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordPublic9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordPublic9.java");

		String str = cu.getSource();
		String completeBehind = "pub";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordReturn1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn1.java",
		"public class CompletionKeywordReturn1 {\n" +
		"	void foo() {\n" +
		"		re\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"return[KEYWORD]{return, null, null, return, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordReturn2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn2.java",
		"public class CompletionKeywordReturn2 {\n" +
		"	void foo() {\n" +
		"		if(re\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
public void testCompletionKeywordReturn3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn3.java",
		"public class CompletionKeywordReturn3 {\n" +
		"	re\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"re[POTENTIAL_METHOD_DECLARATION]{re, LCompletionKeywordReturn3;, ()V, re, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordReturn4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn4.java",
		"public class CompletionKeywordReturn4 {\n" +
		"	void foo() {\n" +
		"		#\n" +
		"		re\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"return[KEYWORD]{return, null, null, return, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordReturn5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn5.java",
		"public class CompletionKeywordReturn5 {\n" +
		"	void foo() {\n" +
		"		#\n" +
		"		if(re\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
public void testCompletionKeywordReturn6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src2/CompletionKeywordReturn6.java",
		"#\n" +
		"public class CompletionKeywordReturn6 {\n" +
		"	re\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"re[POTENTIAL_METHOD_DECLARATION]{re, LCompletionKeywordReturn6;, ()V, re, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=176364
public void testCompletionKeywordReturn7() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
 		"    switch (i) {\n" +
 		"      case 0:\n" +
 		"        re\n" +
 		"    }\n" +
		"  }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "re";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"return[KEYWORD]{return, null, null, return, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordStatic1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic1.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic10.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic2.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic3.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic4.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordStatic5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic5.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic6.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic7.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic8.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:static    completion:static    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStatic9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStatic9.java");

		String str = cu.getSource();
		String completeBehind = "sta";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordStrictfp1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp1.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:strictfp    completion:strictfp    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStrictfp2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp2.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordStrictfp3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp3.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:strictfp    completion:strictfp    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStrictfp4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp4.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordStrictfp5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp5.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:strictfp    completion:strictfp    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStrictfp6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp6.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordStrictfp7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp7.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:strictfp    completion:strictfp    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordStrictfp8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordStrictfp8.java");

		String str = cu.getSource();
		String completeBehind = "stric";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSuper1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper1.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper10.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper11.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper12() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src2/CompletionKeywordSuper12.java",
            "public class CompletionKeywordSuper12 {\n"+
            "	public CompletionKeywordSuper12() {\n"+
            "		#\n"+
            "		sup\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "sup";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

    assertResults(
            "SuperClass[TYPE_REF]{SuperClass, , LSuperClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"SuperInterface[TYPE_REF]{SuperInterface, , LSuperInterface;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"super[KEYWORD]{super, null, null, super, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED)+"}\n" +
			"super[METHOD_REF<CONSTRUCTOR>]{super(), Ljava.lang.Object;, ()V, super, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordSuper2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper2.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper3.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper4.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper5.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper6() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src2/CompletionKeywordSuper6.java",
            "public class CompletionKeywordSuper6 {\n"+
            "	public CompletionKeywordSuper6() {\n"+
            "		sup\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

    String str = this.wc.getSource();
    String completeBehind = "sup";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "expectedTypesSignatures=null\n"+
            "expectedTypesKeys=null",
            requestor.getContext());

    assertResults(
            "SuperClass[TYPE_REF]{SuperClass, , LSuperClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"SuperInterface[TYPE_REF]{SuperInterface, , LSuperInterface;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"super[KEYWORD]{super, null, null, super, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED)+"}\n" +
			"super[METHOD_REF<CONSTRUCTOR>]{super(), Ljava.lang.Object;, ()V, super, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionKeywordSuper7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper7.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper8.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:super    completion:super    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSuper9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSuper9.java");

		String str = cu.getSource();
		String completeBehind = "sup";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:SuperClass    completion:SuperClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:SuperInterface    completion:SuperInterface    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSwitch1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch1.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:switch    completion:switch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSwitch2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch2.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSwitch3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch3.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSwitch4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch4.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:switch    completion:switch    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSwitch5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch5.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSwitch6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSwitch6.java");

		String str = cu.getSource();
		String completeBehind = "sw";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized1.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSynchronized10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized10.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized11.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSynchronized12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized12.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized2.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized3.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSynchronized4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized4.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized5.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSynchronized6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized6.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized7.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordSynchronized8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized8.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordSynchronized9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordSynchronized9.java");

		String str = cu.getSource();
		String completeBehind = "syn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:synchronized    completion:synchronized    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis1.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis10.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThis11() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis11.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis12() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis12.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThis13() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis13.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis14() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis14.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
/*
 * bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=42402
 */
public void testCompletionKeywordThis15() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src2/CompletionKeywordThis15.java",
            "public class CompletionKeywordThis15 {\n" +
            "	public class InnerClass {\n" +
            "		public InnerClass() {\n" +
            "			CompletionKeywordThis15 a = CompletionKeywordThis15.this;\n" +
            "		}\n" +
            "	}\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "CompletionKeywordThis15.";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionKeywordThis15.InnerClass[TYPE_REF]{InnerClass, , LCompletionKeywordThis15$InnerClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"class[FIELD_REF]{class, null, Ljava.lang.Class;, class, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n"+
			"this[KEYWORD]{this, null, null, this, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}
public void testCompletionKeywordThis2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis2.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis3.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThis4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis4.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis5.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThis6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis6.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis7.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThis8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis8.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThis9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThis9.java");

		String str = cu.getSource();
		String completeBehind = "thi";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:this    completion:this    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow1.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:throw    completion:throw    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow2.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow3.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow4.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
			"element:throw    completion:throw    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow5.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrow6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrow6.java");

		String str = cu.getSource();
		String completeBehind = "thr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:Throwable    completion:Throwable    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows1.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows2.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThrows3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows3.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows4.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows5.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows6.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordThrows7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows7.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordThrows8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordThrows8.java");

		String str = cu.getSource();
		String completeBehind = "thro";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:throws    completion:throws    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTransient1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient1.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:transient    completion:transient    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTransient2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient2.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTransient3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient3.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:transient    completion:transient    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTransient4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient4.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTransient5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient5.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:transient    completion:transient    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTransient6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient6.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTransient7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient7.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:transient    completion:transient    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTransient8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTransient8.java");

		String str = cu.getSource();
		String completeBehind = "tran";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTrue1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTrue1.java");

		String str = cu.getSource();
		String completeBehind = "tru";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTrue2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTrue2.java");

		String str = cu.getSource();
		String completeBehind = "tru";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:true    completion:true    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTrue3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTrue3.java");

		String str = cu.getSource();
		String completeBehind = "tru";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTrue4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTrue4.java");

		String str = cu.getSource();
		String completeBehind = "tru";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:true    completion:true    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=90615
public void testCompletionKeywordTrue5() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionKeywordTrue5.java",
			"package test;\n" +
			"public class CompletionKeywordTrue5 {\n" +
			"  public void foo() {\n" +
			"    boolean var;\n" +
			"    var = tr\n" +
			"  }\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "tr";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"true[KEYWORD]{true, null, null, true, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=90615
public void testCompletionKeywordTrue6() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionKeywordTrue6.java",
			"package test;\n" +
			"public class CompletionKeywordTrue6 {\n" +
			"  public void foo() {\n" +
			"    boolean var;\n" +
			"    var = \n" +
			"  }\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "var = ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
				"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"foo[METHOD_REF]{foo(), Ltest.CompletionKeywordTrue6;, ()V, foo, null, " +(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"}\n"+
				"var[LOCAL_VARIABLE_REF]{var, null, Z, var, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"}\n"+
				"false[KEYWORD]{false, null, null, false, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED)+"}\n"+
				"true[KEYWORD]{true, null, null, true, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} else {
		assertResults(
				"finalize[METHOD_REF]{finalize(), Ljava.lang.Object;, ()V, finalize, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"foo[METHOD_REF]{foo(), Ltest.CompletionKeywordTrue6;, ()V, foo, null, " +(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"notify[METHOD_REF]{notify(), Ljava.lang.Object;, ()V, notify, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"notifyAll[METHOD_REF]{notifyAll(), Ljava.lang.Object;, ()V, notifyAll, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, ()V, wait, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (J)V, wait, (millis), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"wait[METHOD_REF]{wait(), Ljava.lang.Object;, (JI)V, wait, (millis, nanos), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"}\n"+
				"CompletionKeywordTrue6[TYPE_REF]{CompletionKeywordTrue6, test, Ltest.CompletionKeywordTrue6;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"clone[METHOD_REF]{clone(), Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"getClass[METHOD_REF]{getClass(), Ljava.lang.Object;, ()Ljava.lang.Class;, getClass, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"}\n"+
				"var[LOCAL_VARIABLE_REF]{var, null, Z, var, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"}\n"+
				"false[KEYWORD]{false, null, null, false, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED)+"}\n"+
				"true[KEYWORD]{true, null, null, true, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_TRUE_OR_FALSE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	}
}
public void testCompletionKeywordTry1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry1.java");

		String str = cu.getSource();
		String completeBehind = "tr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:try    completion:try    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTry2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry2.java");

		String str = cu.getSource();
		String completeBehind = "tr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:true    completion:true    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTry3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry3.java");

		String str = cu.getSource();
		String completeBehind = "try";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordTry4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry4.java");

		String str = cu.getSource();
		String completeBehind = "tr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:try    completion:try    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTry5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry5.java");

		String str = cu.getSource();
		String completeBehind = "tr";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:true    completion:true    relevance:"+(R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordTry6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordTry6.java");

		String str = cu.getSource();
		String completeBehind = "try";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordVolatile1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile1.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:volatile    completion:volatile    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordVolatile2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile2.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordVolatile3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile3.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:volatile    completion:volatile    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordVolatile4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile4.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordVolatile5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile5.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:volatile    completion:volatile    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordVolatile6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile6.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordVolatile7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile7.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:volatile    completion:volatile    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordVolatile8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordVolatile8.java");

		String str = cu.getSource();
		String completeBehind = "vol";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordWhile1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile1.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordWhile10() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile10.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordWhile2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile2.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordWhile3() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile3.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordWhile4() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile4.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordWhile5() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile5.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordWhile6() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile6.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionKeywordWhile7() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile7.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordWhile8() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile8.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"",
			requestor.getResults());
}
public void testCompletionKeywordWhile9() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src2", "", "CompletionKeywordWhile9.java");

		String str = cu.getSource();
		String completeBehind = "wh";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:while    completion:while    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionLocalName() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionLocalName.java");

	String str = cu.getSource();
	String completeBehind = "ClassWithComplexName ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:classWithComplexName    completion:classWithComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:complexName2    completion:complexName2    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:name    completion:name    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:withComplexName    completion:withComplexName    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionLocalType1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionLocalType1.java",
		"public class CompletionLocalType1 {\n" +
		"	void foo() {\n" +
		"		class ZZZZ {\n" +
		"			ZZZ\n" +
		"		}\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ZZZ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ZZZ[POTENTIAL_METHOD_DECLARATION]{ZZZ, LZZZZ;, ()V, ZZZ, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n"+
			"ZZZZ[TYPE_REF]{ZZZZ, , LZZZZ;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25815
*/
public void testCompletionMemberType() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionMemberType.java",
            "public class CompletionMemberType {\n"+
            "	public class MemberType {\n"+
            "		public void foo(){\n"+
            "			MemberType var = new MemberType\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "new MemberType";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
		"CompletionMemberType.MemberType[TYPE_REF]{MemberType, , LCompletionMemberType$MemberType;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_EXACT_NAME+ R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionMemberType2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionMemberType2.java",
            "public class CompletionMemberType2 {\n"+
            "	public class MemberException extends Exception {\n"+
            "	}\n"+
            "	void foo() {\n"+
            "		throw new \n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "new ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
		assertResults(
			"",
			requestor.getResults());
	} else {
		assertResults(
			"CompletionMemberType2.MemberException[TYPE_REF]{MemberException, test, Ltest.CompletionMemberType2$MemberException;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXCEPTION+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	}
}
public void testCompletionMemberType3() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionArrayClone.java",
            "public class CompletionMemberType3 {\n"+
            "	public class MemberException extends Exception {\n"+
            "	}\n"+
            "	void foo() {\n"+
            "		throw new MemberE\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "new MemberE";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"CompletionMemberType3.MemberException[TYPE_REF]{MemberException, test, Ltest.CompletionMemberType3$MemberException;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION+ R_UNQUALIFIED + R_NON_RESTRICTED) +"}",
		requestor.getResults());
}
public void testCompletionMessageSendIsParent1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMessageSendIsParent2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent2.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMessageSendIsParent3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent3.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMessageSendIsParent4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent4.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMessageSendIsParent5() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent5.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMessageSendIsParent6() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMessageSendIsParent6.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodDeclaration() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration.java");

	String str = cu.getSource();
	String completeBehind = "eq";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:eqFoo    completion:public int eqFoo(int a, Object b)    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"\n" +
		"element:equals    completion:public boolean equals(Object obj)    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodDeclaration10() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration10.java");

	String str = cu.getSource();
	String completeBehind = "clon";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:CloneNotSupportedException    completion:CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:clone    completion:protected Object clone() throws CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
		requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=80063
public void testCompletionMethodDeclaration11() throws JavaModelException {
	this.wc = getWorkingCopy(
			"/Completion/src/test/CompletionMethodDeclaration11.java",
			"package test;\n" +
			"public class CompletionMethodDeclaration11 {\n" +
			"  private void foo() {\n" +
			"  }\n" +
			"}\n" +
			"class CompletionMethodDeclaration11_2 extends CompletionMethodDeclaration11 {\n" +
			"  fo\n" +
			"}");


	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.wc.getSource();
	String completeBehind = "fo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"fo[POTENTIAL_METHOD_DECLARATION]{fo, Ltest.CompletionMethodDeclaration11_2;, ()V, fo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionMethodDeclaration12() throws JavaModelException {
    this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionMethodDeclaration12.java",
            "package test;\n" +
            "public class CompletionMethodDeclaration12 {\n" +
            "  public void foo() {\n" +
            "  }\n" +
            "}\n" +
            "class CompletionMethodDeclaration12_2 extends CompletionMethodDeclaration12{\n" +
            "  public final void foo() {\n" +
            "  }\n" +
            "}\n" +
            "class CompletionMethodDeclaration12_3 extends CompletionMethodDeclaration12_2 {\n" +
            "  fo\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
            "fo[POTENTIAL_METHOD_DECLARATION]{fo, Ltest.CompletionMethodDeclaration12_3;, ()V, fo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}",
            requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157069
public void testCompletionMethodDeclaration13() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends other.SuperClass {\n" +
 		"  doSom\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/other/SuperClass.java",
		"package other;"+
		"public class SuperClass {\n" +
		"  protected class Sub {}\n" +
 		"  protected Sub doSomething() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "doSom";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"doSomething[METHOD_DECLARATION]{protected Sub doSomething(), Lother.SuperClass;, ()Lother.SuperClass$Sub;, doSomething, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157069
public void testCompletionMethodDeclaration14() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends other.SuperClass {\n" +
 		"  doSom\n" +
		"}\n");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/other/SuperClass.java",
		"package other;"+
		"public class SuperClass extends SuperClass2 {\n" +
		"  private class Sub {}\n" +
		"}\n");
	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/other/SuperClass2.java",
		"package other;"+
		"public class SuperClass2 {\n" +
		"  protected class Sub {}\n" +
 		"  protected Sub doSomething() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "doSom";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"doSomething[METHOD_DECLARATION]{protected Sub doSomething(), Lother.SuperClass2;, ()Lother.SuperClass2$Sub;, doSomething, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157069
public void testCompletionMethodDeclaration15() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends other.SuperClass {\n" +
 		"  doSom\n" +
		"}\n");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/other/SuperClass.java",
		"package other;"+
		"public class SuperClass extends SuperClass2 {\n" +
		"  public class Sub {}\n" +
		"}\n");
	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/other/SuperClass2.java",
		"package other;"+
		"public class SuperClass2 {\n" +
		"  public class Sub {}\n" +
 		"  protected Sub doSomething() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "doSom";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"doSomething[METHOD_DECLARATION]{protected other.SuperClass2.Sub doSomething(), Lother.SuperClass2;, ()Lother.SuperClass2$Sub;, doSomething, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=157069
public void testCompletionMethodDeclaration16() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends other.SuperClass {\n" +
 		"  doSom\n" +
		"}\n");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/other/SuperClass.java",
		"package other;"+
		"public class SuperClass extends SuperClass2 {\n" +
		"  public class Sub {}\n" +
		"}\n");
	this.workingCopies[2] = getWorkingCopy(
		"/Completion/src/other/SuperClass2.java",
		"package other;"+
		"public class SuperClass2 {\n" +
		"  protected class Sub {}\n" +
 		"  protected Sub doSomething() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "doSom";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"doSomething[METHOD_DECLARATION]{protected other.SuperClass2.Sub doSomething(), Lother.SuperClass2;, ()Lother.SuperClass2$Sub;, doSomething, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionMethodDeclaration2() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionMethodDeclaration2.java",
	            "public class CompletionMethodDeclaration2 extends CompletionSuperClass {\n" +
	            "	eq\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "eq";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"eq[POTENTIAL_METHOD_DECLARATION]{eq, LCompletionMethodDeclaration2;, ()V, eq, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"eqFoo[METHOD_DECLARATION]{public int eqFoo(int a, Object b), LCompletionSuperClass;, (ILjava.lang.Object;)I, eqFoo, (a, b), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n" +
			"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
/**
 * Completion should not propose declarations of method already locally implemented
 */
public void testCompletionMethodDeclaration3() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionMethodDeclaration3.java",
	            "public class CompletionMethodDeclaration3 extends CompletionSuperClass {\n" +
	            "	eq\n" +
	            "	\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "eq";
	    int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"eq[POTENTIAL_METHOD_DECLARATION]{eq, LCompletionMethodDeclaration3;, ()V, eq, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
public void testCompletionMethodDeclaration4() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface.java",
	            "public interface CompletionSuperInterface{\n"+
	            "	public int eqFoo(int a,Object b);\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionMethodDeclaration4.java",
	            "public abstract class CompletionMethodDeclaration4 implements CompletionSuperInterface {\n"+
	            "	eq\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "eq";
	    int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"eq[POTENTIAL_METHOD_DECLARATION]{eq, LCompletionMethodDeclaration4;, ()V, eq, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
			"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
			"eqFoo[METHOD_DECLARATION]{public int eqFoo(int a, Object b), LCompletionSuperInterface;, (ILjava.lang.Object;)I, eqFoo, (a, b), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_ABSTRACT_METHOD + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
public void testCompletionMethodDeclaration5() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionMethodDeclaration5.java",
	            "public class CompletionMethodDeclaration5 {\n" +
	            "	public static void main(String[] args) {\n" +
	            "		new CompletionSuperClass() {\n" +
	            "	}\n" +
	            "\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "new CompletionSuperClass() {";
	    int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
			assertResults(
				"[POTENTIAL_METHOD_DECLARATION]{, LCompletionSuperClass;, ()V, , null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
				"clone[METHOD_DECLARATION]{protected Object clone() throws CloneNotSupportedException, Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"eqFoo[METHOD_DECLARATION]{public int eqFoo(int a, Object b), LCompletionSuperClass;, (ILjava.lang.Object;)I, eqFoo, (a, b), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"finalize[METHOD_DECLARATION]{protected void finalize() throws Throwable, Ljava.lang.Object;, ()V, finalize, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"hashCode[METHOD_DECLARATION]{public int hashCode(), Ljava.lang.Object;, ()I, hashCode, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"toString[METHOD_DECLARATION]{public String toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+ "}",
				requestor.getResults());
		} else {
			assertResults(
				"[POTENTIAL_METHOD_DECLARATION]{, LCompletionSuperClass;, ()V, , null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED)+"}\n" +
				"CompletionMethodDeclaration5[TYPE_REF]{CompletionMethodDeclaration5, , LCompletionMethodDeclaration5;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
				"clone[METHOD_DECLARATION]{protected Object clone() throws CloneNotSupportedException, Ljava.lang.Object;, ()Ljava.lang.Object;, clone, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"eqFoo[METHOD_DECLARATION]{public int eqFoo(int a, Object b), LCompletionSuperClass;, (ILjava.lang.Object;)I, eqFoo, (a, b), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"equals[METHOD_DECLARATION]{public boolean equals(Object obj), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"finalize[METHOD_DECLARATION]{protected void finalize() throws Throwable, Ljava.lang.Object;, ()V, finalize, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"hashCode[METHOD_DECLARATION]{public int hashCode(), Ljava.lang.Object;, ()I, hashCode, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED)+"}\n"+
				"toString[METHOD_DECLARATION]{public String toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED)+ "}",
				requestor.getResults());
		}
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
public void testCompletionMethodDeclaration6() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration6.java");

	String str = cu.getSource();
	String completeBehind = "clon";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:CloneNotSupportedException    completion:CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodDeclaration7() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration7.java");

	String str = cu.getSource();
	String completeBehind = "clon";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:CloneNotSupportedException    completion:CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:clone    completion:protected Object clone() throws CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodDeclaration8() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration8.java");

	String str = cu.getSource();
	String completeBehind = "clon";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:CloneNotSupportedException    completion:CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:clone    completion:protected Object clone() throws CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodDeclaration9() throws JavaModelException {

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodDeclaration9.java");

	String str = cu.getSource();
	String completeBehind = "clon";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have one completion",
		"element:CloneNotSupportedException    completion:CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:clone    completion:protected Object clone() throws CloneNotSupportedException    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodThrowsClause() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodThrowsClause.java");

	String str = cu.getSource();
	String completeBehind = "Ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Exception    completion:Exception    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionMethodThrowsClause2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionMethodThrowsClause2.java");

	String str = cu.getSource();
	String completeBehind = "Ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Exception    completion:Exception    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionNonEmptyToken1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionNonEmptyToken1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	int start = cursorLocation - 2;
	int end = start + 4;
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzyy    completion:zzyy    position:["+start+","+end+"]    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResultsWithPosition());
}
public void testCompletionNonStaticFieldRelevance() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionNonStaticFieldRelevance.java");

	String str = cu.getSource();
	String completeBehind = "var.Ii";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:Ii0    completion:Ii0    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "\n" +
			"element:ii1    completion:ii1    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_STATIC+ R_NON_RESTRICTED),
			requestor.getResults());
}
/**
 * Attempt to do completion with a null requestor
 */
public void testCompletionNullRequestor() throws JavaModelException {
	try {
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionFindThisDotField.java");
		cu.codeComplete(5, (CompletionRequestor)null);
	} catch (IllegalArgumentException iae) {
		return;
	}
	assertTrue("Should not be able to do completion with a null requestor", false);
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=24565
*/
public void testCompletionObjectsMethodWithInterfaceReceiver() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionObjectsMethodWithInterfaceReceiver.java");

	String str = cu.getSource();
	String completeBehind = "hash";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:hashCode    completion:hashCode()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_EXACT_EXPECTED_TYPE+ R_NON_RESTRICTED),
		requestor.getResults());
}
/**
 * Ensures that the code assist features works on class files with associated source.
 */
public void testCompletionOnClassFile() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	IClassFile cu = getClassFile("Completion", "zzz.jar", "jarpack1", "X.class");

	String str = cu.getSource();
	String completeBehind = "Obj";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have one class",
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25890
*/
public void testCompletionOnStaticMember1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionOnStaticMember1.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
			requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25890
*/
public void testCompletionOnStaticMember2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionOnStaticMember2.java");

		String str = cu.getSource();
		String completeBehind = "method";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:method1    completion:method1()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
			"element:method2    completion:method2()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
			requestor.getResults());
}
/**
 * Test that an out of bounds index causes an exception.
 */
public void testCompletionOutOfBounds() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionOutOfBounds.java");
	try {
		cu.codeComplete(cu.getSource().length() + 1, requestor);
	} catch (JavaModelException e) {
		return;
	}
	assertTrue("should have failed", false);
}
public void testCompletionPackageAndClass1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "z1.z2.qla0", "Qla3.java");

	String str = cu.getSource();
	String completeBehind = "z1.z2.ql";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:Qla1    completion:z1.z2.Qla1    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_QUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:qla2    completion:z1.z2.qla2    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED + R_NON_RESTRICTED) + "\n" +
			"element:z1.z2.qla0    completion:z1.z2.qla0    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_QUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionPackageAndClass2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "z1.z2.qla0", "Wla.java");

	String str = cu.getSource();
	String completeBehind = "z1.z2.qla0.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:Qla3    completion:Qla3    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "\n" +
			"element:Qla4    completion:Qla4    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "\n" +
			"element:Wla    completion:Wla    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionPrefixFieldName1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixFieldName1.java",
            "public class CompletionPrefixFieldName1 {\n"+
            "	int xBar;\n"+
            "	\n"+
            "	class classFoo {\n"+
            "		int xBar;\n"+
            "		\n"+
            "		public void foo(){\n"+
            "			xBa\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "xBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"xBar[FIELD_REF]{CompletionPrefixFieldName1.this.xBar, LCompletionPrefixFieldName1;, I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
		"xBar[FIELD_REF]{xBar, LCompletionPrefixFieldName1$classFoo;, I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionPrefixFieldName2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixFieldName2.java",
            "public class CompletionPrefixFieldName2 {\n"+
            "	int xBar;\n"+
            "	\n"+
            "	class classFoo {\n"+
            "		int xBar;\n"+
            "		\n"+
            "		public void foo(){\n"+
            "			new CompletionPrefixFieldName2().xBa\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "xBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"xBar[FIELD_REF]{xBar, LCompletionPrefixFieldName2;, I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionPrefixMethodName1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixMethodName1.java",
            "public class CompletionPrefixMethodName1 {\n"+
           "	int xBar(){}\n"+
           "	\n"+
           "	class classFoo {\n"+
           "		int xBar(){}\n"+
           "		\n"+
           "		public void foo(){\n"+
           "			xBa\n"+
           "		}\n"+
           "	}\n"+
           "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "xBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"xBar[METHOD_REF]{CompletionPrefixMethodName1.this.xBar(), LCompletionPrefixMethodName1;, ()I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
		"xBar[METHOD_REF]{xBar(), LCompletionPrefixMethodName1$classFoo;, ()I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionPrefixMethodName2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixMethodName2.java",
            "public class CompletionPrefixMethodName2 {\n"+
            "	int xBar(){}\n"+
            "	\n"+
            "	class classFoo {\n"+
            "		int xBar(){}\n"+
            "		\n"+
            "		public void foo(){\n"+
            "			new CompletionPrefixMethodName2().xBa\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "xBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"xBar[METHOD_REF]{xBar(), LCompletionPrefixMethodName2;, ()I, xBar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionPrefixMethodName3() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionPrefixMethodName2.java",
            "public class CompletionPrefixMethodName3 {\n"+
            "	int xBar(int a, int b){}\n"+
            "	\n"+
            "	class classFoo {\n"+
            "		int xBar(int a, int b){}\n"+
            "		\n"+
            "		public void foo(){\n"+
            "			xBar(1,\n"+
            "		}\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, false, true);
    String str = this.wc.getSource();
    String completeBehind = "xBar(1,";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    int rStart1 = str.lastIndexOf("xBar(1,");
	int rEnd1 = rStart1 + "xBar(1,".length();
	int rStart2 = str.lastIndexOf("xBar(1,") + "xBar(1,".length();
	int rEnd2 = rStart2;

	int tStart = str.lastIndexOf("xBar(1,") + "xBar(1,".length();
	int tEnd = tStart;

	assertResults(
		"xBar[METHOD_REF]{CompletionPrefixMethodName3.this.xBar(1,, LCompletionPrefixMethodName3;, (II)I, xBar, (a, b), replace["+rStart1+", "+rEnd1+"], token["+tStart+", "+tEnd+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED)+"}\n"+
		"xBar[METHOD_REF]{, LCompletionPrefixMethodName3$classFoo;, (II)I, xBar, (a, b), replace["+rStart2+", "+rEnd2+"], token["+tStart+", "+tEnd+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionQualifiedAllocationType1() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionQualifiedAllocationType1.java",
            "public class CompletionQualifiedAllocationType1 {\n"+
            "	public class YYY {\n"+
            "	}\n"+
            "	void foo(){\n"+
            "		this.new YYY\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "YYY";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
		"CompletionQualifiedAllocationType1.YYY[TYPE_REF]{YYY, , LCompletionQualifiedAllocationType1$YYY;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=26677
*/
public void testCompletionQualifiedExpectedType() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/test/CompletionQualifiedExpectedType.java",
            "import pack1.PX;\n"+
            "\n"+
            "public class CompletionQualifiedExpectedType {\n"+
            "	void foo() {\n"+
            "		pack2.PX var = new \n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "new ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    if(CompletionEngine.NO_TYPE_COMPLETION_ON_EMPTY_TOKEN) {
    	assertResults(
	            "PX[TYPE_REF]{pack2.PX, pack2, Lpack2.PX;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE+ R_NON_RESTRICTED)+ "}",
				requestor.getResults());
    } else {
	    assertResults(
	            "CompletionQualifiedExpectedType[TYPE_REF]{CompletionQualifiedExpectedType, test, Ltest.CompletionQualifiedExpectedType;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
				"PX[TYPE_REF]{pack2.PX, pack2, Lpack2.PX;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE+ R_NON_RESTRICTED)+ "}",
				requestor.getResults());
    }
}
/**
 * Complete the type "Repeated", "RepeatedOtherType from "Repeated".
 */
public void testCompletionRepeatedType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionRepeatedType.java");

	String str = cu.getSource();
	String completeBehind = "/**/CompletionRepeated";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);
	assertEquals(
		"should have two types",
		"element:CompletionRepeatedOtherType    completion:CompletionRepeatedOtherType    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:CompletionRepeatedType    completion:CompletionRepeatedType    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25591
*/
public void testCompletionReturnInInitializer() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionReturnInInitializer.java");

	String str = cu.getSource();
	String completeBehind = "eq";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:equals    completion:equals()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionReturnStatementIsParent1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionReturnStatementIsParent1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zz00    completion:zz00    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz00M    completion:zz00M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz01    completion:zz01    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz01M    completion:zz01M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz02    completion:zz02    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz02M    completion:zz02M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz10    completion:zz10    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz10M    completion:zz10M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz11    completion:zz11    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz11M    completion:zz11M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz12    completion:zz12    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz12M    completion:zz12M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz20    completion:zz20    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz20M    completion:zz20M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz21    completion:zz21    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz21M    completion:zz21M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz22    completion:zz22    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zz22M    completion:zz22M()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzOb    completion:zzOb    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzObM    completion:zzObM()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionReturnStatementIsParent2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionReturnStatementIsParent2.java");

	String str = cu.getSource();
	String completeBehind = "xx";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:XX00    completion:XX00    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX01    completion:XX01    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX02    completion:XX02    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX10    completion:XX10    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX11    completion:XX11    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX12    completion:XX12    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX20    completion:XX20    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX21    completion:XX21    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:XX22    completion:XX22    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
/*
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=66908
 */
public void testCompletionSameClass() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionSameClass.java");

	String str = cu.getSource();
	String completeBehind = "(CompletionSameClas";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
			"element:CompletionSameClass    completion:CompletionSameClass    relevance:" + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionSameSuperClass() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionSameSuperClass.java",
            "public class CompletionSameSuperClass extends A {\n" +
            "	class Inner extends A {\n" +
            "		void foo(int bar){\n" +
            "			bar\n" +
            "		}\n" +
            "	}	\n" +
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "bar";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"bar[FIELD_REF]{CompletionSameSuperClass.this.bar, LA;, I, bar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"}\n"+
		"bar[FIELD_REF]{this.bar, LA;, I, bar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"}\n"+
		"bar[METHOD_REF]{CompletionSameSuperClass.this.bar(), LA;, ()V, bar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"}\n"+
		"bar[LOCAL_VARIABLE_REF]{bar, null, I, bar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n"+
		"bar[METHOD_REF]{bar(), LA;, ()V, bar, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
		requestor.getResults());
}
public void testCompletionStaticMethod1() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethod1.java",
	            "public class CompletionStaticMethod1 extends TypeWithAMethodAndAStaticMethod {\n"+
	            "	void bar(){\n"+
	            "		new TypeWithAMethodAndAStaticMethod(){\n"+
	            "			class Inner1 extends TypeWithAMethodAndAStaticMethod {\n"+
	            "				void bar(){\n"+
	            "					foo\n"+
	            "				}\n"+
	            "			}\n"+
	            "		};\n"+
	            "	}\n"+
	            "	\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo0[METHOD_REF]{CompletionStaticMethod1.this.foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_REF]{foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED) + "}\n" +
				"foo[METHOD_REF]{CompletionStaticMethod1.foo(), LTypeWithAMethodAndAStaticMethod;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED) + "}\n" +
				"foo[METHOD_REF]{foo(), LTypeWithAMethodAndAStaticMethod;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_NAME + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration1() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration1.java",
	            "public class CompletionStaticMethodDeclaration1 extends TypeWithAMethodAndAStaticMethod {\n"+
	            "	foo\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LCompletionStaticMethodDeclaration1;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration2() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration2.java",
	            "public class CompletionStaticMethodDeclaration2 {\n" +
	            "	class Inner1 extends TypeWithAMethodAndAStaticMethod {\n" +
	            "		foo\n" +
	            "	}\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LCompletionStaticMethodDeclaration2$Inner1;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration3() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration3.java",
	            "public class CompletionStaticMethodDeclaration3 {\n" +
	            "	static class Inner1 extends TypeWithAMethodAndAStaticMethod {\n" +
	            "		foo\n" +
	            "	}\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LCompletionStaticMethodDeclaration3$Inner1;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration4() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration4.java",
	            "public class CompletionStaticMethodDeclaration4 {\n" +
	            "	void bar() {\n" +
	            "		class Local1 extends TypeWithAMethodAndAStaticMethod {\n" +
	            "			foo\n" +
	            "		}\n" +
	            "	}\n" +
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LLocal1;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration5() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration5.java",
	            "public class CompletionStaticMethodDeclaration5 {\n"+
	            "	void bar() {\n"+
	            "		static class Local1 extends TypeWithAMethodAndAStaticMethod {\n"+
	            "			foo\n"+
	            "		}\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LLocal1;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionStaticMethodDeclaration6() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/TypeWithAMethodAndAStaticMethod .java",
	            "public class TypeWithAMethodAndAStaticMethod {\n"+
	            "	public static void foo(){}\n"+
	            "	public void foo0(){}\n"+
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionStaticMethodDeclaration6.java",
	            "public class CompletionStaticMethodDeclaration6 {\n"+
	            "	void bar() {\n"+
	            "		new TypeWithAMethodAndAStaticMethod() {\n"+
	            "			foo\n"+
	            "		};\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "foo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo[POTENTIAL_METHOD_DECLARATION]{foo, LTypeWithAMethodAndAStaticMethod;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"foo0[METHOD_DECLARATION]{public void foo0(), LTypeWithAMethodAndAStaticMethod;, ()V, foo0, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_METHOD_OVERIDE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType() throws JavaModelException {
	ICompilationUnit superClass = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType.java",
	            "public class CompletionSuperType extends CompletionSuperClass.");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuperClass.";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperClass.Inner[TYPE_REF]{Inner, , LCompletionSuperClass$Inner;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_CLASS+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType2() throws JavaModelException {
	ICompilationUnit superClass = null;
	ICompilationUnit superClass2 = null;
	ICompilationUnit superInterface = null;
	ICompilationUnit superInterface2 = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		superClass2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass2.java",
	            "public class CompletionSuperClass2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		superInterface = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface.java",
	            "public interface CompletionSuperInterface{\n" +
	            "	public int eqFoo(int a,Object b);\n" +
	            "}");

		superInterface2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface2.java",
	            "public interface CompletionSuperInterface2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType2.java",
	            "public class CompletionSuperType2 extends CompletionSuper");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuper";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperClass[TYPE_REF]{CompletionSuperClass, , LCompletionSuperClass;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_CLASS + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"CompletionSuperClass2[TYPE_REF]{CompletionSuperClass2, , LCompletionSuperClass2;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_CLASS + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
		if(superClass2 != null) {
			superClass2.discardWorkingCopy();
		}
		if(superInterface != null) {
			superInterface.discardWorkingCopy();
		}
		if(superInterface2 != null) {
			superInterface2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType3() throws JavaModelException {
	ICompilationUnit superClass = null;
	ICompilationUnit superClass2 = null;
	ICompilationUnit superInterface = null;
	ICompilationUnit superInterface2 = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		superClass2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass2.java",
	            "public class CompletionSuperClass2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		superInterface = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface.java",
	            "public interface CompletionSuperInterface{\n" +
	            "	public int eqFoo(int a,Object b);\n" +
	            "}");

		superInterface2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface2.java",
	            "public interface CompletionSuperInterface2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType3.java",
	            "public class CompletionSuperType3 implements CompletionSuper");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuper";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperInterface[TYPE_REF]{CompletionSuperInterface, , LCompletionSuperInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"CompletionSuperInterface2[TYPE_REF]{CompletionSuperInterface2, , LCompletionSuperInterface2;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
		if(superClass2 != null) {
			superClass2.discardWorkingCopy();
		}
		if(superInterface != null) {
			superInterface.discardWorkingCopy();
		}
		if(superInterface2 != null) {
			superInterface2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType4() throws JavaModelException {
	ICompilationUnit superClass2 = null;
	try {
		superClass2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass2.java",
	            "public class CompletionSuperClass2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType4.java",
	            "public class CompletionSuperType4 extends CompletionSuperClass2.Inner");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuperClass2.Inner";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperClass2.InnerInterface[TYPE_REF]{InnerInterface, , LCompletionSuperClass2$InnerInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED)+ "}\n"+
			"CompletionSuperClass2.InnerClass[TYPE_REF]{InnerClass, , LCompletionSuperClass2$InnerClass;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_CLASS + R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass2 != null) {
			superClass2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType5() throws JavaModelException {
	ICompilationUnit superInterface2 = null;
	try {
		superInterface2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface2.java",
	            "public interface CompletionSuperInterface2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType5.java",
	            "public class CompletionSuperType5 implements CompletionSuperInterface2.Inner");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuperInterface2.Inner";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperInterface2.InnerClass[TYPE_REF]{InnerClass, , LCompletionSuperInterface2$InnerClass;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
			"CompletionSuperInterface2.InnerInterface[TYPE_REF]{InnerInterface, , LCompletionSuperInterface2$InnerInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superInterface2 != null) {
			superInterface2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType6() throws JavaModelException {
	ICompilationUnit superClass = null;
	ICompilationUnit superClass2 = null;
	ICompilationUnit superInterface = null;
	ICompilationUnit superInterface2 = null;
	try {
		superClass = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass.java",
	            "public class CompletionSuperClass{\n" +
	            "	public class Inner {}\n" +
	            "	public int eqFoo(int a,Object b){\n" +
	            "		return 1;\n" +
	            "	}\n" +
	            "}");

		superClass2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass2.java",
	            "public class CompletionSuperClass2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		superInterface = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface.java",
	            "public interface CompletionSuperInterface{\n" +
	            "	public int eqFoo(int a,Object b);\n" +
	            "}");

		superInterface2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface2.java",
	            "public interface CompletionSuperInterface2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType6.java",
	            "public interface CompletionSuperType6 extends CompletionSuper");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuper";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"CompletionSuperInterface[TYPE_REF]{CompletionSuperInterface, , LCompletionSuperInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_INTERFACE + R_NON_RESTRICTED)+"}\n" +
				"CompletionSuperInterface2[TYPE_REF]{CompletionSuperInterface2, , LCompletionSuperInterface2;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_INTERFACE+ R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		if(superClass != null) {
			superClass.discardWorkingCopy();
		}
		if(superClass2 != null) {
			superClass2.discardWorkingCopy();
		}
		if(superInterface != null) {
			superInterface.discardWorkingCopy();
		}
		if(superInterface2 != null) {
			superInterface2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType7() throws JavaModelException {
	ICompilationUnit superClass2 = null;
	try {
		superClass2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperClass2.java",
	            "public class CompletionSuperClass2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType7.java",
	            "public interface CompletionSuperType7 extends CompletionSuperClass2.Inner");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuperClass2.Inner";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperClass2.InnerClass[TYPE_REF]{InnerClass, , LCompletionSuperClass2$InnerClass;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
			"CompletionSuperClass2.InnerInterface[TYPE_REF]{InnerInterface, , LCompletionSuperClass2$InnerInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superClass2 != null) {
			superClass2.discardWorkingCopy();
		}
	}
}
public void testCompletionSuperType8() throws JavaModelException {
	ICompilationUnit superInterface2 = null;
	try {
		superInterface2 = getWorkingCopy(
	            "/Completion/src/CompletionSuperInterface2.java",
	            "public interface CompletionSuperInterface2 {\n" +
	            "	public class InnerClass {}\n" +
	            "	public interface InnerInterface {}\n" +
	            "}");

		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionSuperType8.java",
	            "public interface CompletionSuperType8 extends CompletionSuperInterface2.Inner");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "CompletionSuperInterface2.Inner";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
			"CompletionSuperInterface2.InnerClass[TYPE_REF]{InnerClass, , LCompletionSuperInterface2$InnerClass;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n" +
			"CompletionSuperInterface2.InnerInterface[TYPE_REF]{InnerInterface, , LCompletionSuperInterface2$InnerInterface;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_INTERFACE+ R_NON_RESTRICTED)+"}",
			requestor.getResults());
	} finally {
		if(superInterface2 != null) {
			superInterface2.discardWorkingCopy();
		}
	}
}
public void testCompletionThrowStatement() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionThrowStatement.java");

	String str = cu.getSource();
	String completeBehind = "Ex";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Exception    completion:Exception    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXCEPTION + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionToplevelType1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "p3", "CompletionToplevelType1.java");

		String str = cu.getSource();
		String completeBehind = "CompletionToplevelType1";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:CompletionToplevelType1    completion:CompletionToplevelType1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionType1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionType1.java");

	String str = cu.getSource();
	String completeBehind = "CT1";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:CT1    completion:CT1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n"+
		"element:CT1    completion:q2.CT1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME+ R_NON_RESTRICTED)+"\n"+
		"element:CompletionType1    completion:CompletionType1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CAMEL_CASE + R_UNQUALIFIED  + R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionUnaryOperator1() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnaryOperator1.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
public void testCompletionUnaryOperator2() throws JavaModelException {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnaryOperator2.java");

		String str = cu.getSource();
		String completeBehind = "var";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:var1    completion:var1    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
			"element:var2    completion:var2    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_EXACT_EXPECTED_TYPE + R_NON_RESTRICTED)+"\n" +
			"element:var3    completion:var3    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
			requestor.getResults());
}
/*
 * bug : http://dev.eclipse.org/bugs/show_bug.cgi?id=24440
 */
public void testCompletionUnresolvedEnclosingType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnresolvedEnclosingType.java");

	String str = cu.getSource();
	String completeBehind = "new ZZZ(";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertTrue(
		requestor.getResults().length() == 0);
}
public void testCompletionUnresolvedFieldType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnresolvedFieldType.java");

	String str = cu.getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:bar    completion:bar    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC+ R_NON_RESTRICTED) +"\n"+
		"element:barPlus    completion:barPlus()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionUnresolvedParameterType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnresolvedParameterType.java");

	String str = cu.getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:bar    completion:bar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC+ R_NON_RESTRICTED) +"\n"+
		"element:barPlus    completion:barPlus()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionUnresolvedReturnType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionUnresolvedReturnType.java");

	String str = cu.getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:bar    completion:bar()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC+ R_NON_RESTRICTED) +"\n"+
		"element:barPlus    completion:barPlus()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionUnresolvedSuperclass() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" + 
		"  public void foo(pack.Bin4 b) {\n" + 
		"    b.bar\n" + 
		"  }\n" + 
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"barPlus[METHOD_REF]{barPlus(), Lpack.Bin4;, ()V, barPlus, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionUnresolvedSuperinteface() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" + 
		"  public void foo(pack.Bin5 b) {\n" + 
		"    b.bar\n" + 
		"  }\n" + 
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"barPlus[METHOD_REF]{barPlus(), Lpack.Bin5;, ()V, barPlus, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testCompletionVariableInitializerInInitializer1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInInitializer1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInInitializer2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInInitializer2.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInInitializer3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInInitializer3.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInInitializer4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInInitializer4.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInMethod1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInMethod1.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInMethod2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInMethod2.java");

	String str = cu.getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:zzObject    completion:zzObject    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzboolean    completion:zzboolean    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzdouble    completion:zzdouble    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzint    completion:zzint    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:zzlong    completion:zzlong    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInMethod3() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInMethod3.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableInitializerInMethod4() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableInitializerInMethod4.java");

	String str = cu.getSource();
	String completeBehind = "Objec";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:Object    completion:Object    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25811
*/
public void testCompletionVariableName1() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableName1.java");

	String str = cu.getSource();
	String completeBehind = "TEST_FOO_MyClass ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:class1    completion:class1    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:foo_MyClass    completion:foo_MyClass    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:myClass    completion:myClass    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:test_FOO_MyClass    completion:test_FOO_MyClass    relevance:"+(R_DEFAULT + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVariableName10() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName10.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName10 {\n"+
	            "	void foo(){\n"+
	            "		FooBar fo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "fo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"foBar[VARIABLE_DECLARATION]{foBar, null, LFooBar;, foBar, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"foBarsuf[VARIABLE_DECLARATION]{foBarsuf, null, LFooBar;, foBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"fooBar[VARIABLE_DECLARATION]{fooBar, null, LFooBar;, fooBar, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}\n"+
				"fooBarsuf[VARIABLE_DECLARATION]{fooBarsuf, null, LFooBar;, fooBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableName11() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName11.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName11 {\n"+
	            "	void foo(){\n"+
	            "		FooBar pr\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "pr";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"preBar[VARIABLE_DECLARATION]{preBar, null, LFooBar;, preBar, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preFooBar[VARIABLE_DECLARATION]{preFooBar, null, LFooBar;, preFooBar, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preBarsuf[VARIABLE_DECLARATION]{preBarsuf, null, LFooBar;, preBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX+ R_NON_RESTRICTED)+"}\n"+
				"preFooBarsuf[VARIABLE_DECLARATION]{preFooBarsuf, null, LFooBar;, preFooBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableName12() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName12.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName12 {\n"+
	            "	void foo(){\n"+
	            "		FooBar prethe\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "prethe";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"preTheBar[VARIABLE_DECLARATION]{preTheBar, null, LFooBar;, preTheBar, null, "+(R_DEFAULT + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preTheFooBar[VARIABLE_DECLARATION]{preTheFooBar, null, LFooBar;, preTheFooBar, null, "+(R_DEFAULT + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preTheBarsuf[VARIABLE_DECLARATION]{preTheBarsuf, null, LFooBar;, preTheBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"preTheFooBarsuf[VARIABLE_DECLARATION]{preTheFooBarsuf, null, LFooBar;, preTheFooBarsuf, null, "+(R_DEFAULT + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableName13() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName13.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName13 {\n"+
	            "	void foo(){\n"+
	            "		FooBar prefo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "prefo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"preFoBar[VARIABLE_DECLARATION]{preFoBar, null, LFooBar;, preFoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preFoBarsuf[VARIABLE_DECLARATION]{preFoBarsuf, null, LFooBar;, preFoBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"preFooBar[VARIABLE_DECLARATION]{preFooBar, null, LFooBar;, preFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}\n"+
				"preFooBarsuf[VARIABLE_DECLARATION]{preFooBarsuf, null, LFooBar;, preFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableName14() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName14.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName14 {\n"+
	            "	void foo(){\n"+
	            "		FooBar pretheFo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "pretheFo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"preTheFoBar[VARIABLE_DECLARATION]{preTheFoBar, null, LFooBar;, preTheFoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preTheFoBarsuf[VARIABLE_DECLARATION]{preTheFoBarsuf, null, LFooBar;, preTheFoBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"preTheFooBar[VARIABLE_DECLARATION]{preTheFooBar, null, LFooBar;, preTheFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}\n"+
				"preTheFooBarsuf[VARIABLE_DECLARATION]{preTheFooBarsuf, null, LFooBar;, preTheFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=215975
public void testCompletionVariableName14_2() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName14.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName14 {\n"+
	            "	void foo(){\n"+
	            "		FooBar prethefo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "prethefo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"preThefoBar[VARIABLE_DECLARATION]{preThefoBar, null, LFooBar;, preThefoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preThefoFooBar[VARIABLE_DECLARATION]{preThefoFooBar, null, LFooBar;, preThefoFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"}\n"+
				"preThefoBarsuf[VARIABLE_DECLARATION]{preThefoBarsuf, null, LFooBar;, preThefoBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"preThefoFooBarsuf[VARIABLE_DECLARATION]{preThefoFooBarsuf, null, LFooBar;, preThefoFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=128045
public void testCompletionVariableName15() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName15.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName15 {\n"+
	            "	void foo(){\n"+
	            "		FooBar pro\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "pro";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"proBar[VARIABLE_DECLARATION]{proBar, null, LFooBar;, proBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"proFooBar[VARIABLE_DECLARATION]{proFooBar, null, LFooBar;, proFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"proBarsuf[VARIABLE_DECLARATION]{proBarsuf, null, LFooBar;, proBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"proFooBarsuf[VARIABLE_DECLARATION]{proFooBarsuf, null, LFooBar;, proFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_CASE + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName16() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		Object ;\n"+
            "		foo = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo[VARIABLE_DECLARATION]{foo, null, Ljava.lang.Object;, foo, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName17() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		Object foo1;\n"+
            "		/*here*/Object ;\n"+
            "		Object foo3;\n"+
            "		foo1 = null;\n"+
            "		foo2 = null;\n"+
            "		foo3 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName18() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		Object ;\n"+
            "		foo = Test.class;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo[VARIABLE_DECLARATION]{foo, null, Ljava.lang.Object;, foo, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName19() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		Object ;\n"+
            "		object = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25811
*/
public void testCompletionVariableName2() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableName2.java");

	String str = cu.getSource();
	String completeBehind = "Test_Bar_MyClass ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:bar_MyClass    completion:bar_MyClass    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:class1    completion:class1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:myClass    completion:myClass    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n" +
		"element:test_Bar_MyClass    completion:test_Bar_MyClass    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
		requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName20() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		class X {\n"+
            "		  Object foo1 = foo2;\n"+
            "		  void bar() {\n"+
            "		    foo1 = null;\n"+
            "		    Object foo3 = foo4;\n"+
            "		    foo3 = null;\n"+
            "		  }\n"+
            "		}\n"+
            "		foo5 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo4[VARIABLE_DECLARATION]{foo4, null, Ljava.lang.Object;, foo4, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo5[VARIABLE_DECLARATION]{foo5, null, Ljava.lang.Object;, foo5, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName21() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		{\n"+
            "		  /*here*/Object ;\n"+
            "		  foo1 = null;\n"+
            "		}\n"+
            "		foo2 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo1[VARIABLE_DECLARATION]{foo1, null, Ljava.lang.Object;, foo1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName22() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		Object foo1;\n"+
            "		/*here*/Object ;\n"+
            "		{\n"+
            "		  Object foo3;\n"+
            "		  foo1 = null;\n"+
            "		  foo2 = null;\n"+
            "		  foo3 = null;\n"+
            "		}\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName23() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		foo1 = null;\n"+
            "		#\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo1[VARIABLE_DECLARATION]{foo1, null, Ljava.lang.Object;, foo1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName24() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		#\n"+
            "		foo1 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo1[VARIABLE_DECLARATION]{foo1, null, Ljava.lang.Object;, foo1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName25() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		#\n"+
            "		foo1 = null;\n"+
            "		#\n"+
            "		foo2 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo1[VARIABLE_DECLARATION]{foo1, null, Ljava.lang.Object;, foo1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName26() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		#\n"+
            "		foo1 = null;\n"+
            "		#\n"+
            "		foo2 = null;\n"+
            "		#\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo1[VARIABLE_DECLARATION]{foo1, null, Ljava.lang.Object;, foo1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName27() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		Object foo0 = null;\n"+
            "		foo0 = null;\n"+
            "		#\n"+
            "		class X {\n"+
            "		  Object foo1 = foo2;\n"+
            "		  void bar() {\n"+
            "		    foo1 = null;\n"+
            "		    Object foo3 = foo4;\n"+
            "		    foo3 = null;\n"+
            "		  }\n"+
            "		}\n"+
            "		foo5 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo2[VARIABLE_DECLARATION]{foo2, null, Ljava.lang.Object;, foo2, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo4[VARIABLE_DECLARATION]{foo4, null, Ljava.lang.Object;, foo4, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo5[VARIABLE_DECLARATION]{foo5, null, Ljava.lang.Object;, foo5, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName28() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		Object foo1 = null;\n"+
            "		foo1.foo2 = null;\n"+
            "		foo3.foo4 = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"foo3[VARIABLE_DECLARATION]{foo3, null, Ljava.lang.Object;, foo3, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName29() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void foo(){\n"+
            "		/*here*/Object ;\n"+
            "		class X {\n"+
            "			void bar1() {\n"+
            "				var1 = null;\n"+
            "			}\n"+
            "			void bar2() {\n"+
            "				Object var2 = null;\n"+
            "				var2 = null;\n"+
            "			}\n"+
            "			void bar3() {\n"+
            "				Object var3 = null;\n"+
            "				{\n"+
            "					var3 = null;\n"+
            "					Object var4 = null;\n"+
            "				}\n"+
            "				var4 = null;\n"+
            "			}\n"+
            "		}\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
    		"var1[VARIABLE_DECLARATION]{var1, null, Ljava.lang.Object;, var1, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"var4[VARIABLE_DECLARATION]{var4, null, Ljava.lang.Object;, var4, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
public void testCompletionVariableName3() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"p1,p2"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"s1,s2"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		CompletionTestsRequestor requestor = new CompletionTestsRequestor();
		ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableName3.java");

		String str = cu.getSource();
		String completeBehind = "OneName ";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		cu.codeComplete(cursorLocation, requestor);

		assertEquals(
			"element:name    completion:name    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:names1    completion:names1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:names2    completion:names2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:oneName    completion:oneName    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"\n"+
			"element:oneNames1    completion:oneNames1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:oneNames2    completion:oneNames2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1Name    completion:p1Name    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1Names1    completion:p1Names1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1Names2    completion:p1Names2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1OneName    completion:p1OneName    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1OneNames1    completion:p1OneNames1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p1OneNames2    completion:p1OneNames2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_PREFIX + R_NAME_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2Name    completion:p2Name    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2Names1    completion:p2Names1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2Names2    completion:p2Names2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NAME_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2OneName    completion:p2OneName    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2OneNames1    completion:p2OneNames1    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"\n"+
			"element:p2OneNames2    completion:p2OneNames2    relevance:"+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_PREFIX + R_NAME_SUFFIX+ R_NON_RESTRICTED),
			requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName30() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	public Test(){\n"+
            "		Object ;\n"+
            "		foo = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo[VARIABLE_DECLARATION]{foo, null, Ljava.lang.Object;, foo, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName31() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	{\n"+
            "		Object ;\n"+
            "		foo = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo[VARIABLE_DECLARATION]{foo, null, Ljava.lang.Object;, foo, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=150228
public void testCompletionVariableName32() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar(Object ) {\n"+
            "		foo = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Object ";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"object[VARIABLE_DECLARATION]{object, null, Ljava.lang.Object;, object, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"foo[VARIABLE_DECLARATION]{foo, null, Ljava.lang.Object;, foo, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162743
public void testCompletionVariableName33() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar() {\n"+
            "		/**/int v\n"+
            "		variable = null;\n"+
            "		variable = null;\n"+
            "		variable = null;\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/**/int v";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"variable[VARIABLE_DECLARATION]{variable, null, I, variable, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162968
public void testCompletionVariableName34() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	int vDefined;\n"+
            "	void bar() {\n"+
            "		/**/int v\n"+
            "		System.out.println(vUnknown);\n"+
            "		System.out.println(vUnknown);\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/**/int v";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"vUnknown[VARIABLE_DECLARATION]{vUnknown, null, I, vUnknown, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_NAME_FIRST_PREFIX + R_NAME_LESS_NEW_CHARACTERS + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=166570
public void testCompletionVariableName35() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar() {\n"+
            "		Test2 zzz\n"+
            "		int zzzzz = 0;\n"+
            "	}\n"+
            "}");

	this.workingCopies[1] = getWorkingCopy(
            "/Completion/src/test/Test2.java",
            "package test;\n"+
            "public class Test2 {\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "Test2 zzz";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"zzzTest2[VARIABLE_DECLARATION]{zzzTest2, null, Ltest.Test2;, zzzTest2, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=175834
public void testCompletionVariableName36() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar() {\n"+
            "		/**/int varzz\n"+
            "		{\n"+
            "			int varzz1 = 0;\n"+
            "			varzz1 = 0;\n"+
            "		}\n"+
            "		int varzz2 = 0;\n"+
            "		#\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/**/int varzz";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=176190
public void testCompletionVariableName37() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar() {\n"+
            "		int va\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "va";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=176190
public void testCompletionVariableName38() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
            "/Completion/src/test/Test.java",
            "package test;\n"+
            "public class Test {\n"+
            "	void bar() {\n"+
            "		boolean va\n"+
            "	}\n"+
            "}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "va";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=137452
public void testCompletionVariableName39() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test.java",
			"package test;\n"+
			"public class AutoCompleteBug {\n"+
			"    interface I {\n"+
			"        void doIt();\n"+
			"    }\n"+
			"    class C1 implements I {\n"+
			"        public void doIt() {\n"+
			"        }\n"+
			"    }\n"+
			"    class C2 extends C1 {\n"+
			"        /*here*/public void doIt() {\n"+
			"			super.doIt();\n"+
			"        }\n"+
			"    }\n"+
			"}");

    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.workingCopies[0].getSource();
    String completeBehind = "/*here*/public void doIt";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"",
			requestor.getResults());
}
public void testCompletionVariableName4() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName4.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName4 {\n"+
            "	void foo(){\n"+
            "		FooBar the\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "the";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"theBar[VARIABLE_DECLARATION]{theBar, null, LFooBar;, theBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"theFooBar[VARIABLE_DECLARATION]{theFooBar, null, LFooBar;, theFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
public void testCompletionVariableName5() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName5.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName5 {\n"+
            "	void foo(){\n"+
            "		FooBar theFo\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "theFo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"theFoBar[VARIABLE_DECLARATION]{theFoBar, null, LFooBar;, theFoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"theFooBar[VARIABLE_DECLARATION]{theFooBar, null, LFooBar;, theFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=215975
public void testCompletionVariableName5_2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName5.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName5 {\n"+
            "	void foo(){\n"+
            "		FooBar thefo\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "thefo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"thefoBar[VARIABLE_DECLARATION]{thefoBar, null, LFooBar;, thefoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"thefoFooBar[VARIABLE_DECLARATION]{thefoFooBar, null, LFooBar;, thefoFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
public void testCompletionVariableName6() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName6.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName6 {\n"+
            "	void foo(){\n"+
            "		FooBar theBa\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "theBa";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"theBaFooBar[VARIABLE_DECLARATION]{theBaFooBar, null, LFooBar;, theBaFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"theBar[VARIABLE_DECLARATION]{theBar, null, LFooBar;, theBar, null, "+(R_DEFAULT  + R_INTERESTING +  + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=215975
public void testCompletionVariableName6_2() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName6.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName6 {\n"+
            "	void foo(){\n"+
            "		FooBar theba\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "theba";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
    		"thebaBar[VARIABLE_DECLARATION]{thebaBar, null, LFooBar;, thebaBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"thebaFooBar[VARIABLE_DECLARATION]{thebaFooBar, null, LFooBar;, thebaFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
public void testCompletionVariableName7() throws JavaModelException {
	this.wc = getWorkingCopy(
            "/Completion/src/CompletionVariableName7.java",
            "class FooBar {\n"+
            "}\n"+
            "public class CompletionVariableName7 {\n"+
            "	void foo(){\n"+
            "		FooBar fo\n"+
            "	}\n"+
            "}");


    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
    String str = this.wc.getSource();
    String completeBehind = "fo";
    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

    assertResults(
			"foBar[VARIABLE_DECLARATION]{foBar, null, LFooBar;, foBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
			"fooBar[VARIABLE_DECLARATION]{fooBar, null, LFooBar;, fooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}

public void testCompletionVariableName8() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName8.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName8 {\n"+
	            "	void foo(){\n"+
	            "		FooBar the\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "the";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"theBar[VARIABLE_DECLARATION]{theBar, null, LFooBar;, theBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"theFooBar[VARIABLE_DECLARATION]{theFooBar, null, LFooBar;, theFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"theBarsuf[VARIABLE_DECLARATION]{theBarsuf, null, LFooBar;, theBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"theFooBarsuf[VARIABLE_DECLARATION]{theFooBarsuf, null, LFooBar;, theFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_NAME_FIRST_SUFFIX + R_CASE + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableName9() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName9.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName9 {\n"+
	            "	void foo(){\n"+
	            "		FooBar theFo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "theFo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"theFoBar[VARIABLE_DECLARATION]{theFoBar, null, LFooBar;, theFoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"theFoBarsuf[VARIABLE_DECLARATION]{theFoBarsuf, null, LFooBar;, theFoBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"theFooBar[VARIABLE_DECLARATION]{theFooBar, null, LFooBar;, theFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED)+"}\n"+
				"theFooBarsuf[VARIABLE_DECLARATION]{theFooBarsuf, null, LFooBar;, theFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_LESS_NEW_CHARACTERS + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=215975
public void testCompletionVariableName9_2() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object argumentPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_PREFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,"pre"); //$NON-NLS-1$
	Object localPrefixPreviousValue = options.get(JavaCore.CODEASSIST_LOCAL_SUFFIXES);
	options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,"suf"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.wc = getWorkingCopy(
	            "/Completion/src/CompletionVariableName9.java",
	            "class FooBar {\n"+
	            "}\n"+
	            "public class CompletionVariableName9 {\n"+
	            "	void foo(){\n"+
	            "		FooBar thefo\n"+
	            "	}\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "thefo";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    assertResults(
				"thefoBar[VARIABLE_DECLARATION]{thefoBar, null, LFooBar;, thefoBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"thefoFooBar[VARIABLE_DECLARATION]{thefoFooBar, null, LFooBar;, thefoFooBar, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NON_RESTRICTED)+"}\n"+
				"thefoBarsuf[VARIABLE_DECLARATION]{thefoBarsuf, null, LFooBar;, thefoBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}\n"+
				"thefoFooBarsuf[VARIABLE_DECLARATION]{thefoFooBarsuf, null, LFooBar;, thefoFooBarsuf, null, "+(R_DEFAULT  + R_INTERESTING + R_CASE + R_NAME_FIRST_SUFFIX + R_NON_RESTRICTED)+"}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_LOCAL_PREFIXES,argumentPrefixPreviousValue);
		options.put(JavaCore.CODEASSIST_LOCAL_SUFFIXES,localPrefixPreviousValue);
		JavaCore.setOptions(options);
	}
}
public void testCompletionVariableNameOfArray1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionVariableNameOfArray1.java",
		"public class CompletionVariableNameOfArray1 {\n"+
		"	Object[] ob\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ob";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"objects[VARIABLE_DECLARATION]{objects, null, [Ljava.lang.Object;, objects, null, " + (R_DEFAULT  + R_INTERESTING + R_CASE+ R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}
public void testCompletionVariableNameOfArray2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionVariableNameOfArray2.java",
		"public class CompletionVariableNameOfArray2 {\n"+
		"	Class[] cl\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cl";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"classes[VARIABLE_DECLARATION]{classes, null, [Ljava.lang.Class;, classes, null, " + (R_DEFAULT  + R_INTERESTING + R_CASE+ R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}
public void testCompletionVariableNameOfArray3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionVariableNameOfArray3.java",
		"public class CompletionVariableNameOfArray3 {\n"+
		"	Object[][] ob\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ob";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"objects[VARIABLE_DECLARATION]{objects, null, [[Ljava.lang.Object;, objects, null, " + (R_DEFAULT  + R_INTERESTING + R_CASE+ R_NAME_LESS_NEW_CHARACTERS + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}
public void testCompletionVariableNameOfArray4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/CompletionVariableNameOfArray4.java",
		"public class CompletionVariableNameOfArray4 {\n"+
		"	Objectz[] ob\n"+
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ob";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"",
		requestor.getResults());
}
public void testCompletionVariableNameUnresolvedType() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVariableNameUnresolvedType.java");

	String str = cu.getSource();
	String completeBehind = "ob";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have no completion",
		"",
		requestor.getResults());
}
public void testCompletionVisibilityCheckDisabled() throws JavaModelException {
	String visibilityCheckID = "org.eclipse.jdt.core.codeComplete.visibilityCheck";
	Hashtable options = JavaCore.getOptions();
	Object visibilityCheckPreviousValue = options.get(visibilityCheckID);
	options.put(visibilityCheckID,"disabled");
	JavaCore.setOptions(options);

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVisibilityCheck.java");

	String str = cu.getSource();
	String completeBehind = "x.p";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	options.put(visibilityCheckID,visibilityCheckPreviousValue);
	JavaCore.setOptions(options);
	assertEquals(
		"should have three methods",
		"element:privateFoo    completion:privateFoo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"\n" +
		"element:protectedFoo    completion:protectedFoo()    relevance:"+(R_DEFAULT+ R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"\n" +
		"element:publicFoo    completion:publicFoo()    relevance:"+(R_DEFAULT+ R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionVisibilityCheckEnabled() throws JavaModelException {
	String visibilityCheckID = "org.eclipse.jdt.core.codeComplete.visibilityCheck";
	Hashtable options = JavaCore.getOptions();
	Object visibilityCheckPreviousValue = options.get(visibilityCheckID);
	options.put(visibilityCheckID,"enabled");
	JavaCore.setOptions(options);

	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVisibilityCheck.java");

	String str = cu.getSource();
	String completeBehind = "x.p";
	int cursorLocation = str.indexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	options.put(visibilityCheckID,visibilityCheckPreviousValue);
	JavaCore.setOptions(options);
	assertEquals(
		"should have two methods",
		"element:protectedFoo    completion:protectedFoo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"\n" +
		"element:publicFoo    completion:publicFoo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC+ R_NON_RESTRICTED),
		requestor.getResults());
}
/*
* http://dev.eclipse.org/bugs/show_bug.cgi?id=25815
*/
public void testCompletionVoidMethod() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionVoidMethod.java");

	String str = cu.getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"element:foo    completion:foo()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED)+"\n" +
		"element:foo1    completion:foo1()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:foo3    completion:foo3()    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED+ R_NON_RESTRICTED),
		requestor.getResults());
}
public void testCompletionWithBinaryFolder() throws JavaModelException {
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	ICompilationUnit cu= getCompilationUnit("Completion", "src", "", "CompletionWithBinaryFolder.java");

	String str = cu.getSource();
	String completeBehind = "My";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	cu.codeComplete(cursorLocation, requestor);

	assertEquals(
		"should have two completions",
		"element:MyClass    completion:MyClass    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"\n" +
		"element:mypackage    completion:mypackage    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING+ R_NON_RESTRICTED),
		requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95167
public void testCompletionWithProblem1() throws JavaModelException {
	ICompilationUnit aType = null;
	try {
		aType = getWorkingCopy(
	            "/Completion/src/test/AType.java",
	            "package test;\n" +
	            "public class AType{\n"+
	            "  void foo(Unknown var) {\n"+
	            "  }\n"+
	            "}");

	    this.wc = getWorkingCopy(
	            "/Completion/src/test/Test.java",
	            "package test;\n" +
	            "public class Test{\n"+
	            "  void foo() {\n"+
	            "    AType a = null;\n"+
	            "    a.zz\n"+
	            "  }\n"+
	            "}");


	    CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	    String str = this.wc.getSource();
	    String completeBehind = "a.zz";
	    int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	    this.wc.codeComplete(cursorLocation, requestor, this.wcOwner);

	    // no completion must be found
		assertResults(
	            "",
	            requestor.getResults());

		// no error must be found
		assertResults(
	            "",
	            requestor.getProblem());
	} finally {
		if(aType != null) {
			aType.discardWorkingCopy();
		}
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public void foo() {\n" +
		"                TestConstructor1[] var = new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public void foo(TestConstructor1[] var) {\n" +
		"                foo(new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public TestConstructor1[] foo() {\n" +
		"                return new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public Test(TestConstructor1[] var) {}\n" +
		"        public void foo() {\n" +
		"                new Test(new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public void foo(TestConstructor1[] var) {\n" +
		"                if (var == new TestConstructor) {}\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=262932
public void testConstructor6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public void foo(Object o) {\n" +
		"                o = (TestConstructor1[])new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, (I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
public void testConstructor7() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public void foo(Object o) {\n" +
		"                new TestConstructor\n" +
		"        }\n" +
		"}");
	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestConstructor1.java",
		"package test;"+
		"public class TestConstructor1 {\n" +
		"        public TestConstructor1(int[] i) {\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, true, true);
	requestor.allowAllRequiredProposals();
	NullProgressMonitor monitor = new NullProgressMonitor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestConstructor";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner, monitor);

	assertResults(
			"TestConstructor1[CONSTRUCTOR_INVOCATION]{(), Ltest.TestConstructor1;, ([I)V, TestConstructor1, (i), "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}\n" +
			"   TestConstructor1[TYPE_REF]{TestConstructor1, test, Ltest.TestConstructor1;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck1() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.workingCopies = new ICompilationUnit[3];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  ZZZTy\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"}");

		this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType2.java",
			"package deprecation;"+
			"/** @deprecated */\n"+
			"public class ZZZType2 {\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZTy";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"ZZZTy[POTENTIAL_METHOD_DECLARATION]{ZZZTy, Ldeprecation.Test;, ()V, ZZZTy, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"ZZZType1[TYPE_REF]{ZZZType1, deprecation, Ldeprecation.ZZZType1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"ZZZType2[TYPE_REF]{ZZZType2, deprecation, Ldeprecation.ZZZType2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck10() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  public void bar1(){}\n"+
			"  /** @deprecated */\n"+
			"  public void bar2(){}\n"+
			"  void foo() {"+
			"    bar\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "bar";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"bar1[METHOD_REF]{bar1(), Ldeprecation.Test;, ()V, bar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"bar2[METHOD_REF]{bar2(), Ldeprecation.Test;, ()V, bar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck11() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  public int bar1;\n"+
			"  /** @deprecated */\n"+
			"  public int bar2;\n"+
			"  void foo() {"+
			"    bar\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "bar";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"bar1[FIELD_REF]{bar1, Ldeprecation.Test;, I, bar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"bar2[FIELD_REF]{bar2, Ldeprecation.Test;, I, bar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck12() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  public int bar1;\n"+
			"  /** @deprecated */\n"+
			"  public int bar2;\n"+
			"  void foo() {"+
			"    bar\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "bar";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"bar1[FIELD_REF]{bar1, Ldeprecation.Test;, I, bar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"bar2[FIELD_REF]{bar2, Ldeprecation.Test;, I, bar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck13() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  class Inner1 {}\n"+
			"  /** @deprecated */\n"+
			"  class Inner2 {}\n"+
			"  void foo() {"+
			"    Inn\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "Inn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"Test.Inner1[TYPE_REF]{Inner1, deprecation, Ldeprecation.Test$Inner1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"Test.Inner2[TYPE_REF]{Inner2, deprecation, Ldeprecation.Test$Inner2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck14() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  class Inner1 {}\n"+
			"  /** @deprecated */\n"+
			"  class Inner2 {}\n"+
			"  void foo() {"+
			"    Inn\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "Inn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"Test.Inner1[TYPE_REF]{Inner1, deprecation, Ldeprecation.Test$Inner1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"Test.Inner2[TYPE_REF]{Inner2, deprecation, Ldeprecation.Test$Inner2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck15() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZType1.foo\n"+
			"  }"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"/** @deprecated */\n"+
			"public class ZZZType1 {\n"+
			"  public static int foo1;\n"+
			"  public static int foo2;\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.foo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck16() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"/** @deprecated */\n"+
			"public class ZZZType1 {\n"+
			"}"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZTy\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZTy";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"ZZZType1[TYPE_REF]{ZZZType1, deprecation, Ldeprecation.ZZZType1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127628
public void testDeprecationCheck17() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  Bug127628Ty\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "Bug127628Ty";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"Bug127628Ty[POTENTIAL_METHOD_DECLARATION]{Bug127628Ty, Ldeprecation.Test;, ()V, Bug127628Ty, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"Bug127628Type1.Bug127628TypeInner1[TYPE_REF]{deprecation.Bug127628Type1.Bug127628TypeInner1, deprecation, Ldeprecation.Bug127628Type1$Bug127628TypeInner1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"Bug127628Type2.Bug127628TypeInner2[TYPE_REF]{deprecation.Bug127628Type2.Bug127628TypeInner2, deprecation, Ldeprecation.Bug127628Type2$Bug127628TypeInner2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"Bug127628Type1[TYPE_REF]{Bug127628Type1, deprecation, Ldeprecation.Bug127628Type1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck2() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.workingCopies = new ICompilationUnit[3];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  ZZZTy\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"}");

		this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType2.java",
			"package deprecation;"+
			"/** @deprecated */\n"+
			"public class ZZZType2 {\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZTy";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"ZZZTy[POTENTIAL_METHOD_DECLARATION]{ZZZTy, Ldeprecation.Test;, ()V, ZZZTy, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
				"ZZZType1[TYPE_REF]{ZZZType1, deprecation, Ldeprecation.ZZZType1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck3() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZType1.fo\n"+
			"  }"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public static void foo1(){}\n"+
			"  /** @deprecated */\n"+
			"  public static void foo2(){}\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo1[METHOD_REF]{foo1(), Ldeprecation.ZZZType1;, ()V, foo1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
				"foo2[METHOD_REF]{foo2(), Ldeprecation.ZZZType1;, ()V, foo2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck4() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZType1.fo\n"+
			"  }"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public static void foo1(){}\n"+
			"  /** @deprecated */\n"+
			"  public static void foo2(){}\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo1[METHOD_REF]{foo1(), Ldeprecation.ZZZType1;, ()V, foo1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck5() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  ZZZType1.Inn\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public class Inner1 {}\n"+
			"  /** @deprecated */\n"+
			"  public class Inner2 {}\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.Inn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"ZZZType1.Inner1[TYPE_REF]{Inner1, deprecation, Ldeprecation.ZZZType1$Inner1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
				"ZZZType1.Inner2[TYPE_REF]{Inner2, deprecation, Ldeprecation.ZZZType1$Inner2;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck6() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  ZZZType1.Inn\n"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public class Inner1 {}\n"+
			"  /** @deprecated */\n"+
			"  public class Inner2 {}\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.Inn";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"ZZZType1.Inner1[TYPE_REF]{Inner1, deprecation, Ldeprecation.ZZZType1$Inner1;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck7() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZType1.fo\n"+
			"  }"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public static int foo1;\n"+
			"  /** @deprecated */\n"+
			"  public static int foo2;\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo1[FIELD_REF]{foo1, Ldeprecation.ZZZType1;, I, foo1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
				"foo2[FIELD_REF]{foo2, Ldeprecation.ZZZType1;, I, foo2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck8() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.ENABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  void foo() {"+
			"    ZZZType1.fo\n"+
			"  }"+
			"}");

		this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/deprecation/ZZZType1.java",
			"package deprecation;"+
			"public class ZZZType1 {\n"+
			"  public static int foo1;\n"+
			"  /** @deprecated */\n"+
			"  public static int foo2;\n"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "ZZZType1.fo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"foo1[FIELD_REF]{foo1, Ldeprecation.ZZZType1;, I, foo1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=127296
public void testDeprecationCheck9() throws JavaModelException {
	Hashtable options = JavaCore.getOptions();
	Object optionValue = options.get(JavaCore.CODEASSIST_DEPRECATION_CHECK);
	options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, JavaCore.DISABLED); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {

		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/deprecation/Test.java",
			"package deprecation;"+
			"public class Test {\n"+
			"  public void bar1(){}\n"+
			"  /** @deprecated */\n"+
			"  public void bar2(){}\n"+
			"  void foo() {"+
			"    bar\n"+
			"  }"+
			"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
		String str = this.workingCopies[0].getSource();
		String completeBehind = "bar";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		assertResults(
				"bar1[METHOD_REF]{bar1(), Ldeprecation.Test;, ()V, bar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
				"bar2[METHOD_REF]{bar2(), Ldeprecation.Test;, ()V, bar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		options.put(JavaCore.CODEASSIST_DEPRECATION_CHECK, optionValue);
		JavaCore.setOptions(options);
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=144858
public void testDuplicateLocals1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	void foo() {\n" +
		"		int x = 0;\n" +
		"		TestString x = null;\n" +
		"		x.bar;\n" +
		"	}\n" +
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestString.java",
		"package test;"+
		"public class TestString {\n" +
		"	public void bar() {\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), Ltest.TestString;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=144858
public void testDuplicateLocals2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public static void main(String[] args) {\n" +
		"                int x = 2;\n" +
		"                try {\n" +
		"                \n" +
		"                } catch(TestException x) {\n" +
		"                        x.bar\n" +
		"                } catch(Exception e) {\n" +
		"                }\n" +
		"        }\n" +
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestException.java",
		"package test;"+
		"public class TestException extends Exception {\n" +
		"	public void bar() {\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), Ltest.TestException;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=144858
public void testDuplicateLocals3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public static void main(String[] args) {\n" +
		"                int x = x = 0;\n" +
		"                if (true) {\n" +
		"                        TestString x = x.bar\n" +
		"                }\n" +
		"        }\n" +
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestString.java",
		"package test;"+
		"public class TestString {\n" +
		"	public void bar() {\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), Ltest.TestString;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_VOID + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=144858
public void testDuplicateLocals4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public static void main(String[] args) {\n" +
		"                for (int i = 0; i < 10; i++) {\n" +
		"                        for (TestString i = null; i.bar < 5;)  {\n" +
		"                                // do something\n" +
		"                        }\n" +
		"                }\n" +
		"        }\n" +
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestString.java",
		"package test;"+
		"public class TestString {\n" +
		"	public void bar() {\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), Ltest.TestString;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_VOID + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=144858
public void testDuplicateLocals5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        public static void main(String[] args) {\n" +
		"                for (int i = 0; i < 10; i++) {\n" +
		"                        for (TestString i = null; ;)  {\n" +
		"                                i.bar // do something\n" +
		"                        }\n" +
		"                }\n" +
		"        }\n" +
		"}");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/TestString.java",
		"package test;"+
		"public class TestString {\n" +
		"	public void bar() {\n" +
		"	}\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), Ltest.TestString;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=165662
public void testDuplicateLocalsType1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"     class Local {\n" +
		"        void foo() {}\n" +
		"     }\n" +
		"     {\n" +
		"        class Local {\n" +
		"                Local(int i) {\n" +
		"                        this.init(i);\n" +
		"                }\n" +
		"				 void init(int i) {}\n" +
		"                public void bar() {}\n" +
		"        }\n" +
		"        Local l = new Local(0);\n" +
		"        l.bar\n" +
		"     }\n" +
		"  }\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar[METHOD_REF]{bar(), LLocal;, ()V, bar, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=165662
public void testDuplicateLocalsType2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"        void foo() {\n" +
		"                class Local {\n" +
		"                        void foo() {\n" +
		"                        }\n" +
		"                }\n" +
		"                {\n" +
		"                        class Local {\n" +
		"                               Local(int i) {\n" +
		"                                       this.init(i);\n" +
		"                                       this.bar();\n" +
		"                               }\n" +
		"				 				void init(int i) {}\n" +
		"                        		void bar() {\n" +
		"                        		}\n" +
		"                        }\n" +
		"                        Local l = new Local(0);\n" +
		"                }\n" +
		"                Local l = new Local();\n" +
		"                l.foo\n" +
		"        }\n" +
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"foo[METHOD_REF]{foo(), LLocal;, ()V, foo, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=139937
public void testEvaluationContextCompletion() throws JavaModelException {
	class EvaluationContextCompletionRequestor extends CompletionRequestor {
		public boolean acceptContext;
		public void accept(CompletionProposal proposal) {
			// Do nothing
		}
		public void acceptContext(CompletionContext context) {
			this.acceptContext = context != null;
		}
	}
	String start = "";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();
    EvaluationContextCompletionRequestor rc = new EvaluationContextCompletionRequestor();
	context.codeComplete(start, start.length(), rc);

	assertTrue("acceptContext() method isn't call", rc.acceptContext);
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=140123
public void testEvaluationContextCompletion2() throws JavaModelException {
	class EvaluationContextCompletionRequestor extends CompletionRequestor {
		public boolean acceptContext;
		public boolean beginReporting;
		public boolean endReporting;

		public void accept(CompletionProposal proposal) {
			// Do nothing
		}
		public void acceptContext(CompletionContext context) {
			this.acceptContext = context != null;
		}

		public void beginReporting() {
			this.beginReporting = true;
			super.beginReporting();
		}

		public void endReporting() {
			this.endReporting =  true;
			super.endReporting();
		}
	}
	String start = "";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();
    EvaluationContextCompletionRequestor rc = new EvaluationContextCompletionRequestor();
	context.codeComplete(start, start.length(), rc);

	assertTrue("acceptContext() method isn't call", rc.acceptContext);
	assertTrue("beginReporting() method isn't call", rc.beginReporting);
	assertTrue("endReporting() method isn't call", rc.endReporting);
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=140123
public void testEvaluationContextCompletion3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TestEvaluationContextCompletion3.java",
		"package test;"+
		"public class TestEvaluationContextCompletion3 {\n"+
		"}");

	String start = "TestEvaluationContextCompletion3";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, false);
	context.codeComplete(start, start.length(), requestor, this.wcOwner);

	int startOffset = 0;
	int endOffset = start.length();

	assertResults(
			"completion offset="+endOffset+"\n"+
			"completion range=["+startOffset+", "+(endOffset-1)+"]\n"+
			"completion token=\"TestEvaluationContextCompletion3\"\n"+
			"completion token kind=TOKEN_KIND_NAME\n"+
			"expectedTypesSignatures=null\n"+
			"expectedTypesKeys=null\n"+
			"completion token location={STATEMENT_START}",
            requestor.getContext());

	assertResults(
			"TestEvaluationContextCompletion3[TYPE_REF]{test.TestEvaluationContextCompletion3, test, Ltest.TestEvaluationContextCompletion3;, null, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=140123
public void testEvaluationContextCompletion4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TestEvaluationContextCompletion4.java",
		"package test;"+
		"public class TestEvaluationContextCompletion4 {\n"+
		"}");

	String start = "TestEvaluationContextCompletion4";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, false);
	requestor.setIgnored(CompletionProposal.TYPE_REF, true);
	context.codeComplete(start, start.length(), requestor, this.wcOwner);

	int startOffset = 0;
	int endOffset = start.length();

	assertResults(
			"completion offset="+endOffset+"\n"+
			"completion range=["+startOffset+", "+(endOffset-1)+"]\n"+
			"completion token=\"TestEvaluationContextCompletion4\"\n"+
			"completion token kind=TOKEN_KIND_NAME\n"+
			"expectedTypesSignatures=null\n"+
			"expectedTypesKeys=null\n"+
			"completion token location={STATEMENT_START}",
            requestor.getContext());

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=141518
public void testEvaluationContextCompletion5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TestEvaluationContextCompletion5.java",
		"package test;"+
		"public class TestEvaluationContextCompletion5 {\n"+
		"}");

	String start = "someVariable.to";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();

	context.newVariable( "Object", "someVariable", null );

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, false);
	context.codeComplete(start, start.length(), requestor, this.wcOwner);

	int startOffset = start.length() - 2;
	int endOffset = startOffset + 2 ;

	assertResults(
			"completion offset="+endOffset+"\n"+
			"completion range=["+startOffset+", "+(endOffset-1)+"]\n"+
			"completion token=\"to\"\n"+
			"completion token kind=TOKEN_KIND_NAME\n"+
			"expectedTypesSignatures=null\n"+
			"expectedTypesKeys=null\n"+
			"completion token location=UNKNOWN",
            requestor.getContext());

	assertResults(
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, toString, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=179000
public void testEvaluationContextCompletion6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TestEvaluationContextCompletion6.java",
		"package test;"+
		"public class TestEvaluationContextCompletion6 {\n"+
		"}");

	String start = "";
	IJavaProject javaProject = getJavaProject("Completion");
	IEvaluationContext context = javaProject.newEvaluationContext();

	context.newVariable( "Object", "someVariable", null );

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, false, false);
	context.codeComplete(start, start.length(), requestor, this.wcOwner);

	int startOffset = start.length();
	int endOffset = startOffset;

	assertResults(
			"completion offset="+endOffset+"\n"+
			"completion range=["+startOffset+", "+(endOffset)+"]\n"+
			"completion token=\"\"\n"+
			"completion token kind=TOKEN_KIND_NAME\n"+
			"expectedTypesSignatures=null\n"+
			"expectedTypesKeys=null\n"+
			"completion token location={STATEMENT_START}",
            requestor.getContext());

	String varClassSignature = getVarClassSignature(context);

	assertResults(
			"someVariable[FIELD_REF]{someVariable, "+varClassSignature+", Ljava.lang.Object;, someVariable, null, "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports001() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[FIELD_REF]{ZZZ.foo, Ltest.p.ZZZ;, I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports002() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports003() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports004() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports005() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[FIELD_REF]{ZZZ.foo, Ltest.p.ZZZ;, I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports006() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports007() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"import test.p.ZZZ.*;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports009() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"import test.p.ZZZ.*;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports011() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"import test.p.ZZZ.foo;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports013() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"import test.p.ZZZ.foo;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports016() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public class foo {\n" +
			"        public void method() {\n" +
			"            foo\n" +
			"        }\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class Test");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}\n"+
			"Test.foo[TYPE_REF]{foo, test, Ltest.Test$foo;, null, null, ["+start1+", "+end1+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports017() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void foo() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	assertResults(
			"foo[METHOD_REF]{foo(), Ltest.Test;, ()V, foo, null, ["+start1+", "+end1+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports018() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public int foo;\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class Test");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}\n"+
			"foo[FIELD_REF]{foo, Ltest.Test;, I, foo, null, ["+start1+", "+end1+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports019() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        int foo = 0;\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class Test");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}\n"+
			"foo[LOCAL_VARIABLE_REF]{foo, null, I, foo, null, ["+start1+", "+end1+"], "+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED)+"}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports020() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"    public static int foo(int i){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class Test");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}\n"+
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, (I)I, foo, (i), ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports022() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo();\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){}\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo(";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports023() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"/** */\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("/** */");
	int end2 = start2 + "".length();
	assertResults(
			"foo[FIELD_REF]{ZZZ.foo, Ltest.p.ZZZ;, I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports024() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports025() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public int foo;\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports026() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public int foo(){return 0;};\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports027() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public int foo(){return 0;};\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports028() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"import test.p.ZZZ;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZ.java",
			"package test.p;\n" +
			"public class ZZZ {\n" +
			"    public static int foo(){return 0;};\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.foo"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	assertResults(
			"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports030() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();

	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_SUGGEST_STATIC_IMPORTS, JavaCore.DISABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
				"/Completion/src3/test/Test.java",
				"package test;\n" +
				"public class Test {\n" +
				"    public void method() {\n" +
				"        foo\n" +
				"    }\n" +
				"}");

		this.workingCopies[1] = getWorkingCopy(
				"/Completion/src3/test/p/ZZZ.java",
				"package test.p;\n" +
				"public class ZZZ {\n" +
				"    public static int foo(){}\n" +
				"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
		requestor.allowAllRequiredProposals();
		requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

		String str = this.workingCopies[0].getSource();
		String completeBehind = "foo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
		int start1 = str.lastIndexOf("foo") + "".length();
		int end1 = start1 + "foo".length();
		int start2 = str.lastIndexOf("public class");
		int end2 = start2 + "".length();
		assertResults(
				"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
				"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=152123
public void testFavoriteImports031() throws JavaModelException {
	this.oldOptions = JavaCore.getOptions();

	try {
		Hashtable options = new Hashtable(this.oldOptions);
		options.put(JavaCore.CODEASSIST_SUGGEST_STATIC_IMPORTS, JavaCore.ENABLED);
		JavaCore.setOptions(options);

		this.workingCopies = new ICompilationUnit[2];
		this.workingCopies[0] = getWorkingCopy(
				"/Completion/src3/test/Test.java",
				"package test;\n" +
				"public class Test {\n" +
				"    public void method() {\n" +
				"        foo\n" +
				"    }\n" +
				"}");

		this.workingCopies[1] = getWorkingCopy(
				"/Completion/src3/test/p/ZZZ.java",
				"package test.p;\n" +
				"public class ZZZ {\n" +
				"    public static int foo(){}\n" +
				"}");

		CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
		requestor.allowAllRequiredProposals();
		requestor.setFavoriteReferences(new String[]{"test.p.ZZZ.*"});

		String str = this.workingCopies[0].getSource();
		String completeBehind = "foo";
		int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

		int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
		int start1 = str.lastIndexOf("foo") + "".length();
		int end1 = start1 + "foo".length();
		int start2 = str.lastIndexOf("public class");
		int end2 = start2 + "".length();
		assertResults(
				"foo[METHOD_REF]{ZZZ.foo(), Ltest.p.ZZZ;, ()I, foo, null, ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
				"   ZZZ[TYPE_IMPORT]{import test.p.ZZZ;\n, test.p, Ltest.p.ZZZ;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
				requestor.getResults());
	} finally {
		JavaCore.setOptions(this.oldOptions);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=219099
public void testFavoriteImports032() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZA.java",
			"package test.p;\n" +
			"public class ZZZA {\n" +
			"    public static int foo(int i){return 0;};\n" +
			"}");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZB.java",
			"package test.p;\n" +
			"public class ZZZB {\n" +
			"    public static int foo(int i){return 0;};\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZA.*", "test.p.ZZZB.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("foo") + "".length();
	int end1 = start1 + "foo".length();
	int start2 = str.lastIndexOf("public class");
	int end2 = start2 + "".length();
	assertResults(
			"foo[METHOD_REF]{ZZZA.foo(), Ltest.p.ZZZA;, (I)I, foo, (i), ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZA[TYPE_IMPORT]{import test.p.ZZZA;\n, test.p, Ltest.p.ZZZA;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}\n" +
			"foo[METHOD_REF]{ZZZB.foo(), Ltest.p.ZZZB;, (I)I, foo, (i), ["+start1+", "+end1+"], "+(relevance1)+"}\n" +
			"   ZZZB[TYPE_IMPORT]{import test.p.ZZZB;\n, test.p, Ltest.p.ZZZB;, null, null, ["+start2+", "+end2+"], " + (relevance1) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=219099
public void testFavoriteImports033() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"    public static int foo(int i){return 0;};\n" +
			"    public void method() {\n" +
			"        foo\n" +
			"    }\n" +
			"}");

	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZA.java",
			"package test.p;\n" +
			"public class ZZZA {\n" +
			"    public static int foo(int i){return 0;};\n" +
			"}");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src3/test/p/ZZZB.java",
			"package test.p;\n" +
			"public class ZZZB {\n" +
			"    public static int foo(int i){return 0;};\n" +
			"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, false, true);
	requestor.allowAllRequiredProposals();
	requestor.setFavoriteReferences(new String[]{"test.p.ZZZA.*", "test.p.ZZZB.*"});

	String str = this.workingCopies[0].getSource();
	String completeBehind = "foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_NAME + R_UNQUALIFIED + R_NON_RESTRICTED;
	int start = str.lastIndexOf("foo") + "".length();
	int end = start + "foo".length();
	assertResults(
			"foo[METHOD_REF]{foo(), Ltest.Test;, (I)I, foo, (i), ["+start+", "+end+"], " + (relevance) + "}",
			requestor.getResults());
}
public void testInconsistentHierarchy1() throws CoreException, IOException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/p/Test.java",
		"package p;"+
		"public class Test extends Unknown {\n" +
		"  void foo() {\n" +
		"    this.has\n" +
		"  }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.has";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
		"hashCode[METHOD_REF]{hashCode(), Ljava.lang.Object;, ()I, hashCode, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
		requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n"+
		"  void foo() {\n"+
		"    label1 : for(;;) foo();\n"+
		"    label2 : for(;;)\n"+
		"      label3 : for(;;) {\n"+
		"        label4 : for(;;) {\n"+
		"          break lab\n"+
		"        }\n"+
		"      }\n"+
		"  }\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label2[LABEL_REF]{label2, null, null, label2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label3[LABEL_REF]{label3, null, null, label3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label4[LABEL_REF]{label4, null, null, label4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n"+
		"  void foo() {\n"+
		"    #\n"+
		"    label1 : for(;;) foo();\n"+
		"    label2 : for(;;)\n"+
		"      label3 : for(;;) {\n"+
		"        label4 : for(;;) {\n"+
		"          break lab\n"+
		"        }\n"+
		"      }\n"+
		"  }\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label2[LABEL_REF]{label2, null, null, label2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label3[LABEL_REF]{label3, null, null, label3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label4[LABEL_REF]{label4, null, null, label4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n"+
		"  void foo() {\n"+
		"    label1 : for(;;) foo();\n"+
		"    label2 : for(;;)\n"+
		"      label3 : for(;;) {\n"+
		"        label4 : for(;;) {\n"+
		"          break lab\n"+
		"  }\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label2[LABEL_REF]{label2, null, null, label2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label3[LABEL_REF]{label3, null, null, label3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label4[LABEL_REF]{label4, null, null, label4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n"+
		"  void foo() {\n"+
		"    #\n"+
		"    label1 : for(;;) foo();\n"+
		"    label2 : for(;;)\n"+
		"      label3 : for(;;) {\n"+
		"        label4 : for(;;) {\n"+
		"          break lab\n"+
		"  }\n"+
		"}");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label2[LABEL_REF]{label2, null, null, label2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label3[LABEL_REF]{label3, null, null, label3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"label4[LABEL_REF]{label4, null, null, label4, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    #\n" +
 		"    label1 : for(;;) {\n" +
 		"      class X {\n" +
 		"        void foo() {\n" +
 		"          label2 : for(;;) foo();\n" +
 		"        }\n" +
 		"      }\n" +
 		"      continue lab\n" +
 		"    }\n" +
		"  }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label1[LABEL_REF]{label1, null, null, label1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testLabel6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/label/Test.java",
		"package label;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    #\n" +
 		"    label1 : for(;;) {\n" +
 		"      class X {\n" +
 		"        void foo() {\n" +
 		"          label2 : for(;;) {\n" +
 		"            continue lab\n" +
 		"          }\n" +
 		"        }\n" +
 		"      }\n" +
 		"    }\n" +
		"  }\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lab";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"label2[LABEL_REF]{label2, null, null, label2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testMethod1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo(Test2 y) {\n" +
		"    y.bar\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/Test2.java",
		"package test;"+
		"public class Test2 {\n" +
		"  public Object bar1() {}\n" +
		"  public Zork bar2() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "y.bar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"bar1[METHOD_REF]{bar1(), Ltest.Test2;, ()Ljava.lang.Object;, bar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}\n" +
			"bar2[METHOD_REF]{bar2(), Ltest.Test2;, ()LZork;, bar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testMethod2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo(Test2 y) {\n" +
		"    y.bar().foo\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/Test2.java",
		"package test;"+
		"public class Test2 {\n" +
		"  public Zork bar() {}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "y.bar().foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
public void testMethod3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo(Test2 y) {\n" +
		"    y.fbar.foo\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/Test2.java",
		"package test;"+
		"public class Test2 {\n" +
		"  public Zork fBar;\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "y.fbar.foo";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences001() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
 			"      {\n" +
 			"         int varzz1 = 0;\n" +
			"         varzz1 = varzz2;\n" +
			"         {\n" +
			"            int varzz3 = 0;\n" +
			"            varzz3 = varzz4;\n" +
			"            int varzz5 = 0;\n" +
			"         }\n" +
			"         {\n" +
			"            varzz1 = varzz5;\n" +
			"         }\n" +
			"         int zzvarzz = 0;\n" +
			"         /**/varzz\n" +
			"         int varzz6 = 0;\n" +
			"         varzz6 = varzz7;\n" +
			"         {\n" +
			"            int varzz8 = 0;\n" +
			"            varzz8 = varzz9;\n" +
 			"         }\n" +
 			"      }\n" +
 			"      int varzz10 = 0;\n" +
			"      varzz10= varzz11;\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz11[LOCAL_VARIABLE_REF]{varzz11, null, Ljava.lang.Object;, varzz11, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz5[LOCAL_VARIABLE_REF]{varzz5, null, Ljava.lang.Object;, varzz5, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz7[LOCAL_VARIABLE_REF]{varzz7, null, Ljava.lang.Object;, varzz7, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz9[LOCAL_VARIABLE_REF]{varzz9, null, Ljava.lang.Object;, varzz9, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences002() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   {\n" +
			"      {\n" +
 			"         int varzz1 = 0;\n" +
			"         varzz1 = varzz2;\n" +
			"         {\n" +
			"            int varzz3 = 0;\n" +
			"            varzz3 = varzz4;\n" +
			"            int varzz5 = 0;\n" +
			"         }\n" +
			"         {\n" +
			"            varzz1 = varzz5;\n" +
			"         }\n" +
			"         /**/varzz\n" +
			"         int varzz6 = 0;\n" +
			"         varzz6 = varzz7;\n" +
			"         {\n" +
			"            int varzz8 = 0;\n" +
			"            varzz8 = varzz9;\n" +
 			"         }\n" +
 			"      }\n" +
 			"      int varzz10 = 0;\n" +
			"      varzz10= varzz11;\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz11[LOCAL_VARIABLE_REF]{varzz11, null, Ljava.lang.Object;, varzz11, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz5[LOCAL_VARIABLE_REF]{varzz5, null, Ljava.lang.Object;, varzz5, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz7[LOCAL_VARIABLE_REF]{varzz7, null, Ljava.lang.Object;, varzz7, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz9[LOCAL_VARIABLE_REF]{varzz9, null, Ljava.lang.Object;, varzz9, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences003() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
 			"      int varzz1 = 0;\n" +
			"      varzz1 = varzz2;\n" +
			"      new Object() {\n" +
			"         int varzz3 = varzz4;\n" +
			"         {\n" +
			"            varzz3 = varzz5;\n" +
			"            int varzz6 = 0;\n" +
			"            varzz6 = varzz7;\n" +
			"         }\n" +
			"      };\n" +
			"      /**/varzz\n" +
 			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz5[LOCAL_VARIABLE_REF]{varzz5, null, Ljava.lang.Object;, varzz5, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz7[LOCAL_VARIABLE_REF]{varzz7, null, Ljava.lang.Object;, varzz7, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences004() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      int varzz1 = 0;\n" +
		"      varzz1 = varzz2;\n" +
		"      new Object() {\n" +
		"         int varzz3 = varzz4;\n";
	for (int i = 0; i < 100; i++) {
		source += "\n";
	}
	source +=
		"         int varzz5 = varzz6;\n" +
		"         int varzz7 = 0;\n" +
		"         {\n" +
		"            varzz3 = varzz1;\n" +
		"            varzz5 = varzz8;\n" +
		"            int varzz9 = 0;\n" +
		"            varzz9 = varzz10;\n" +
		"         }\n" +
		"      };\n" +
		"      /**/varzz\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz10[LOCAL_VARIABLE_REF]{varzz10, null, Ljava.lang.Object;, varzz10, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz3[LOCAL_VARIABLE_REF]{varzz3, null, Ljava.lang.Object;, varzz3, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz8[LOCAL_VARIABLE_REF]{varzz8, null, Ljava.lang.Object;, varzz8, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences005() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      int varzz1 = 0;\n" +
		"      varzz1 = varzz2;\n" +
		"      new Object() {\n" +
		"         int varzz3 = varzz4;\n";
	for (int i = 0; i < 100; i++) {
		source += "\n";
	}
	source +=
		"         int varzz5 = varzz6;\n" +
		"         int varzz7(){}\n" +
		"         {\n" +
		"            varzz3 = varzz1;\n" +
		"            varzz5 = varzz8;\n" +
		"            int varzz9 = 0;\n" +
		"            varzz9 = varzz10;\n" +
		"         }\n" +
		"      };\n" +
		"      /**/varzz\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz10[LOCAL_VARIABLE_REF]{varzz10, null, Ljava.lang.Object;, varzz10, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz3[LOCAL_VARIABLE_REF]{varzz3, null, Ljava.lang.Object;, varzz3, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz8[LOCAL_VARIABLE_REF]{varzz8, null, Ljava.lang.Object;, varzz8, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences006() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      int varzz1 = 0;\n" +
		"      varzz1 = varzz2;\n" +
		"      new Object() {\n" +
		"         int varzz3 = varzz4;\n";
	for (int i = 0; i < 100; i++) {
		source += "\n";
	}
	source +=
		"         int varzz5 = varzz6;\n" +
		"         int varzz7()\n" +
		"         {\n" +
		"            varzz3 = varzz1;\n" +
		"            varzz5 = varzz8;\n" +
		"            int varzz9 = 0;\n" +
		"            varzz9 = varzz10;\n" +
		"         }\n" +
		"      };\n" +
		"      /**/varzz\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz10[LOCAL_VARIABLE_REF]{varzz10, null, Ljava.lang.Object;, varzz10, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz3[LOCAL_VARIABLE_REF]{varzz3, null, Ljava.lang.Object;, varzz3, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz8[LOCAL_VARIABLE_REF]{varzz8, null, Ljava.lang.Object;, varzz8, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences007() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      int varzz1 = 0;\n" +
		"      varzz1 = varzz2;\n" +
		"      new Object() {\n" +
		"         int varzz3 = varzz4;\n" +
		"         void foo (){\n" +
		"            int varzz5 = 0;\n" +
		"            varzz5 = varzz6;\n" +
		"            /**/varzz\n" +
		"         }\n" +
		"      };\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz3[FIELD_REF]{varzz3, LObject;, I, varzz3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz5[LOCAL_VARIABLE_REF]{varzz5, null, I, varzz5, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences008() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   {\n" +
		"      int varzz1 = 0;\n" +
		"      varzz1 = varzz2;\n" +
		"      new Object() {\n" +
		"         int varzz3 = varzz4;\n" +
		"         {\n" +
		"            int varzz5 = 0;\n" +
		"            varzz5 = varzz6;\n" +
		"            /**/varzz\n" +
		"         }\n" +
		"      };\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz3[FIELD_REF]{varzz3, LObject;, I, varzz3, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz5[LOCAL_VARIABLE_REF]{varzz5, null, I, varzz5, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences009() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      {\n" +
		"         int varzz1 = 0;\n" +
		"         varzz1 = varzz2;\n" +
		"         {\n" +
		"            int varzz3 = 0;\n" +
		"            varzz3 = varzz4;\n" +
		"         }\n" +
		"         /**/varzz\n" +
		"         int varzz5 = 0;\n" +
		"         varzz5 = varzz6;\n" +
		"         {\n" +
		"            int varzz7 = 0;\n" +
		"            varzz7 = varzz8;\n" +
		"         }\n" +
		"      }\n" +
		"      int varzz9 = 0;\n" +
		"      varzz9 = varzz10;\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz10[LOCAL_VARIABLE_REF]{varzz10, null, Ljava.lang.Object;, varzz10, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz8[LOCAL_VARIABLE_REF]{varzz8, null, Ljava.lang.Object;, varzz8, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=162865
public void testNameWithUnresolvedReferences010() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      {\n" +
		"         int varzz1 = 0;\n" +
		"         varzz1 = varzz2;\n" +
		"         {\n" +
		"            int varzz3 = 0;\n" +
		"            varzz3 = varzz4;\n" +
		"         }\n" +
		"         /**/varzz\n" +
		"         int varzz5 = 0;\n" +
		"         varzz5 = varzz6;\n" +
		"         {\n" +
		"            int varzz7 = 0;\n";
	for (int i = 0; i < 100; i++) {
		source += "\n";
	}
	source +=
		"            varzz7 = varzz8;\n" +
		"         }\n" +
		"      }\n" +
		"      int varzz9 = 0;\n" +
		"      varzz9 = varzz10;\n" +
		"   }\n" +
		"}\n";

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/varzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"varzz2[LOCAL_VARIABLE_REF]{varzz2, null, Ljava.lang.Object;, varzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz4[LOCAL_VARIABLE_REF]{varzz4, null, Ljava.lang.Object;, varzz4, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz6[LOCAL_VARIABLE_REF]{varzz6, null, Ljava.lang.Object;, varzz6, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"varzz1[LOCAL_VARIABLE_REF]{varzz1, null, I, varzz1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=176321
public void testNameWithUnresolvedReferences011() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
			"      /**/zzz   zzz1.zzz2\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/zzz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz1[LOCAL_VARIABLE_REF]{zzz1, null, Ljava.lang.Object;, zzz1, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences012() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
			"      zzzlala = 0;\n" +
			"      zzzlabel : {\n" +
			"        /**/zzzla\n" +
			"      }\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/zzzla";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzzlala[LOCAL_VARIABLE_REF]{zzzlala, null, Ljava.lang.Object;, zzzlala, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences013() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
			"      zzzlala = 0;\n" +
			"      zzzlabel1 : {\n" +
			"        /**/zzzla\n" +
			"        {\n" +
			"          break zzzlabel2;\n" +
			"        }\n" +
			"      }\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/zzzla";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzzlala[LOCAL_VARIABLE_REF]{zzzlala, null, Ljava.lang.Object;, zzzlala, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences014() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
			"      {\n" +
			"        break;\n" +
			"      }\n" +
			"      zzznotlabel = 25;\n" +
			"      {\n" +
			"        /**/zzznotla\n" +
			"      }\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "/**/zzznotla";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzznotlabel[LOCAL_VARIABLE_REF]{zzznotlabel, null, Ljava.lang.Object;, zzznotlabel, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences015() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   int foo() {\n" +
			"      zzz1 = 0;\n" +
			"      if (false) return (ZZZ2) var;\n" +
			"      zz\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz1[LOCAL_VARIABLE_REF]{zzz1, null, Ljava.lang.Object;, zzz1, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences016() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   int foo() {\n" +
			"      zzz1 = 0;\n" +
			"      return (zzz2) zz;\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz1[LOCAL_VARIABLE_REF]{zzz1, null, Ljava.lang.Object;, zzz1, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=177174
public void testNameWithUnresolvedReferences017() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src3/test/Test.java",
			"package test;\n" +
			"public class Test {\n" +
			"   void foo() {\n" +
			"      {\n" +
			"         final int zzz1 = 0;\n" +
			"         class Local {\n" +
			"            void bar() {n" +
			"               zzz1 = 24;\n" +
			"               zzz2 = 24;\n" +
			"            }\n" +
			"         }\n" +
			"      }\n" +
			"      zz\n" +
			"   }\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzz2[LOCAL_VARIABLE_REF]{zzz2, null, Ljava.lang.Object;, zzz2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=220171
public void testNameWithUnresolvedReferences018() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];

	String source =
		"package test;\n" +
		"public class Test {\n" +
		"   void foo() {\n" +
		"      boolean zzzz1 = false;\n" +
		"      {\n" +
		"         \n" +
		"      }\n";

	for (int i = 0; i < 47; i++) {
		source += "\n";
	}

	source +=
		"      boolean zzzz2 = false;\n" +
		"      bar((Object)null, null);\n" +
		"      if (zzzz) {}\n" +
		"   }\n" +
		"}";

	this.workingCopies[0] = getWorkingCopy("/Completion/src3/test/Test.java", source);

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);

	String str = this.workingCopies[0].getSource();
	String completeBehind = "zz";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"zzzz1[LOCAL_VARIABLE_REF]{zzzz1, null, Z, zzzz1, null, " + (R_DEFAULT + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_RESOLVED + R_NON_RESTRICTED) + "}\n" +
			"zzzz2[LOCAL_VARIABLE_REF]{zzzz2, null, Z, zzzz2, null, " + (R_DEFAULT + R_INTERESTING + R_EXACT_EXPECTED_TYPE + R_CASE + R_UNQUALIFIED + R_RESOLVED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
public void testParameterNames1() throws CoreException, IOException {
	Hashtable options = JavaCore.getOptions();
	Object timeout = options.get(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC);
	options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC,"2000"); //$NON-NLS-1$

	JavaCore.setOptions(options);

	try {
		this.workingCopies = new ICompilationUnit[1];
		this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/p/Test.java",
			"package p;"+
			"public class Test {\n" +
			"  void foo(doctest.X x) {\n" +
			"    x.fo\n" +
			"  }\n" +
			"}\n");

		addLibrary(
				"Completion",
				"tmpDoc.jar",
				null,
				"tmpDocDoc.zip",
				false);

		CompletionTestsRequestor2 requestor;
		try {
			requestor = new CompletionTestsRequestor2(true);
			String str = this.workingCopies[0].getSource();
			String completeBehind = "x.fo";
			int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
			this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

			assertResults(
				"foo[METHOD_REF]{foo(), Ldoctest.X;, (Ljava.lang.Object;)V, foo, (param), " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED) + "}",
				requestor.getResults());
		} finally {
			removeLibrary("Completion", "tmpDoc.jar");
		}
	} finally {
		options.put(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, timeout);
		JavaCore.setOptions(options);
	}
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=22072
public void testStaticMembers1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
 		"    StaticMembers.\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/StaticMembers.java",
		"package test;"+
		"public class StaticMembers extends SuperStaticMembers {\n" +
		"  public static int staticField;\n" +
 		"  public static int staticMethod() {}\n" +
		"  public class Clazz {}\n" +
		"  public static class StaticClazz {}\n" +
		"}\n");

	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/SuperStaticMembers.java",
			"package test;"+
			"public class SuperStaticMembers {\n" +
			"  public static int superStaticField;\n" +
	 		"  public static int supeStaticMethod() {}\n" +
			"  public class SuperClazz {}\n" +
			"  public static class SuperStaticClazz {}\n" +
			"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "StaticMembers.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"supeStaticMethod[METHOD_REF]{supeStaticMethod(), Ltest.SuperStaticMembers;, ()I, supeStaticMethod, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"superStaticField[FIELD_REF]{superStaticField, Ltest.SuperStaticMembers;, I, superStaticField, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"StaticMembers.Clazz[TYPE_REF]{Clazz, test, Ltest.StaticMembers$Clazz;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"StaticMembers.StaticClazz[TYPE_REF]{StaticClazz, test, Ltest.StaticMembers$StaticClazz;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"class[FIELD_REF]{class, null, Ljava.lang.Class;, class, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"staticField[FIELD_REF]{staticField, Ltest.StaticMembers;, I, staticField, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"staticMethod[METHOD_REF]{staticMethod(), Ltest.StaticMembers;, ()I, staticMethod, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}\n" +
			"this[KEYWORD]{this, null, null, this, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends boole {\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  boole\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boole[POTENTIAL_METHOD_DECLARATION]{boole, Ltest.Test;, ()V, boole, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  class InnerTest {\n" +
		"    boole\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boole[POTENTIAL_METHOD_DECLARATION]{boole, Ltest.Test$InnerTest;, ()V, boole, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType4() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    boole\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType5() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    class InnerTest {\n" +
		"      boole\n" +
		"    }\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boole[POTENTIAL_METHOD_DECLARATION]{boole, LInnerTest;, ()V, boole, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType6() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo(boole) {\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType7() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    new boole\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/booleanClass.java",
		"package test;"+
		"public class booleanClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "boole";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"boolean[KEYWORD]{boolean, null, null, boolean, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"booleanClass[TYPE_REF]{booleanClass, test, Ltest.booleanClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType8() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test extends voi {\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CLASS + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType9() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  voi\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voi[POTENTIAL_METHOD_DECLARATION]{voi, Ltest.Test;, ()V, voi, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"void[KEYWORD]{void, null, null, void, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType10() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  class InnerTest {\n" +
		"    voi\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voi[POTENTIAL_METHOD_DECLARATION]{voi, Ltest.Test$InnerTest;, ()V, voi, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"void[KEYWORD]{void, null, null, void, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType11() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    voi\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType12() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    class InnerTest {\n" +
		"      voi\n" +
		"    }\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voi[POTENTIAL_METHOD_DECLARATION]{voi, LInnerTest;, ()V, voi, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_NON_RESTRICTED) + "}\n" +
			"void[KEYWORD]{void, null, null, void, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType13() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo(voi) {\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"void[KEYWORD]{void, null, null, void, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}\n" +
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99631
public void testType14() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[2];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"  void foo() {\n" +
		"    new voi\n" +
		"  }\n" +
		"}\n");

	this.workingCopies[1] = getWorkingCopy(
		"/Completion/src/test/voidClass.java",
		"package test;"+
		"public class voidClass {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "voi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"voidClass[TYPE_REF]{voidClass, test, Ltest.voidClass;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=221215
public void testInvalidField1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Event {\n" +
		"        public int x;\n" +
		"\n" +
		"        public void handle(Event e) {\n" +
		"                e.x.e.foo();\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "e.x.e.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=221215 - variation
public void testInvalidField2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Event {\n" +
		"        public int x;\n" +
		"\n" +
		"        public void handle(Event e) {\n" +
		"                this.x.e.foo();\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.x.e.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=221215
public void testInvalidMethod1() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Event {\n" +
		"        public int x;\n" +
		"\n" +
		"        public void handle(Event e) {\n" +
		"                e.x.e().foo();\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "e.x.e().";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}
//https://bugs.eclipse.org/bugs/show_bug.cgi?id=221215 - variation
public void testInvalidMethod2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Event {\n" +
		"        public int x;\n" +
		"\n" +
		"        public void handle(Event e) {\n" +
		"                this.x.e().foo();\n" +
		"        }\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.x.e.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=99399 don't propose final types for extends type completion.
public void testCompletionOnExtendFinalClass () throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"class ThisClassIsNotFinal {}" +
		"final class ThisClassIsFinal {}" +
		"public class Event extends test.ThisClassI {\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.ThisClassI";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ThisClassIsNotFinal[TYPE_REF]{ThisClassIsNotFinal, test, Ltest.ThisClassIsNotFinal;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED + R_CLASS) + "}",	
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=99399 don't propose final types for extends type completion.
public void testCompletionOnExtendFinalClass2() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"class ThisClassIsNotFinal {}" +
		"final class ThisClassIsFinal {}" +
		"public class Event extends ThisClassI {\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ThisClassI";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ThisClassIsNotFinal[TYPE_REF]{ThisClassIsNotFinal, test, Ltest.ThisClassIsNotFinal;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_CLASS) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=99399 test to verify that we continue to propose final types
// in other (non extends context).
public void testCompletionOnExtendFinalClass3() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"class ThisClassIsNotFinal {}" +
		"final class ThisClassIsFinal {}" +
		"public class Event extends ThisClassIsFinal {\n" +
		"	void Boo (ThisClassI x) {}\n" +
		"}");
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ThisClassI";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"ThisClassIsFinal[TYPE_REF]{ThisClassIsFinal, test, Ltest.ThisClassIsFinal;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"ThisClassIsNotFinal[TYPE_REF]{ThisClassIsNotFinal, test, Ltest.ThisClassIsNotFinal;, null, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=203060: assert keyword should not be proposed when
// compliance level is set to 1.3
public void test203060a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/KeywordAssert.java",
		"package test;" +
		"public class CompletionKeywordAssert1 {\n" +
		"	void foo() {\n" +
		"		as\n" +
		"	}\n" +
		"}\n");
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "as";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	
	// Save current compliance settings
	Map options = COMPLETION_PROJECT.getOptions(true);
	Object savedOptionCompliance = options.get(CompilerOptions.OPTION_Compliance);
	
	try {
		// Verify that at 1.3 assert is not proposed.
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);
		COMPLETION_PROJECT.setOptions(options);
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
		assertEquals("", requestor.getResults());
	} finally {
		// Restore compliance settings.
		options.put(CompilerOptions.OPTION_Compliance, savedOptionCompliance);
		COMPLETION_PROJECT.setOptions(options);	
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=203060: Verify that assert keyword gets proposed when
//compliance level is set to 1.4
public void test203060b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/KeywordAssert.java",
		"package test;" +
		"public class CompletionKeywordAssert1 {\n" +
		"	void foo() {\n" +
		"		as\n" +
		"	}\n" +
		"}\n");
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "as";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	
	// Save current compliance settings
	Map options = COMPLETION_PROJECT.getOptions(true);
	Object savedOptionCompliance = options.get(CompilerOptions.OPTION_Compliance);
	
	try {
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
		COMPLETION_PROJECT.setOptions(options);
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
		assertEquals(
				"element:assert    completion:assert    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
				requestor.getResults());
	} finally {
		// Restore compliance settings.
		options.put(CompilerOptions.OPTION_Compliance, savedOptionCompliance);
		COMPLETION_PROJECT.setOptions(options);	
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=203060: assert keyword should not be proposed when
//compliance level is set to 1.3 (variation)
public void test203060c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/KeywordAssert.java",
		"package test;" +
		"public class KeywordAssert {\n" +
		"			void foo() {\n" +
		"		switch(0) {\n" +
		"			case 1 :\n" +
		"				ass\n" +
		"		}\n" +
		"	}\n" +
		"}\n");
		
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "as";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	
	// Save current compliance settings
	Map options = COMPLETION_PROJECT.getOptions(true);
	Object savedOptionCompliance = options.get(CompilerOptions.OPTION_Compliance);
	
	try {
		// Verify that at 1.3 assert is not proposed.
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);
		COMPLETION_PROJECT.setOptions(options);
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
		assertEquals("", requestor.getResults());
	} finally {
		// Restore compliance settings.
		options.put(CompilerOptions.OPTION_Compliance, savedOptionCompliance);
		COMPLETION_PROJECT.setOptions(options);	
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=203060: Verify that assert keyword gets proposed when
//compliance level is set to 1.4 (variation)
public void test203060d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/KeywordAssert.java",
			"package test;" +
			"public class KeywordAssert {\n" +
			"			void foo() {\n" +
			"		switch(0) {\n" +
			"			case 1 :\n" +
			"				ass\n" +
			"		}\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor requestor = new CompletionTestsRequestor();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "as";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	
	// Save current compliance settings
	Map options = COMPLETION_PROJECT.getOptions(true);
	Object savedOptionCompliance = options.get(CompilerOptions.OPTION_Compliance);
	
	try {
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
		COMPLETION_PROJECT.setOptions(options);
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
		assertEquals(
				"element:assert    completion:assert    relevance:"+(R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED),
				requestor.getResults());
	} finally {
		// Restore compliance settings.
		options.put(CompilerOptions.OPTION_Compliance, savedOptionCompliance);
		COMPLETION_PROJECT.setOptions(options);	
	}
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
// All the tests with the prefix test269493 are designed to offer coverage for all the changes that went
// into this fix.
public void test269493() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	void foo() {\n" +
			"   for (int i = 0; i < 10; i++)\n" +
			"       ass\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(); 
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ass";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	// Save current compliance settings
	Map options = COMPLETION_PROJECT.getOptions(true);
	Object savedOptionCompliance = options.get(CompilerOptions.OPTION_Compliance);
	
	try {
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
		COMPLETION_PROJECT.setOptions(options);
		this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
		assertResults(
				"assert[KEYWORD]{assert, null, null, assert, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
				requestor.getResults());
	} finally {
		// Restore compliance settings.
		options.put(CompilerOptions.OPTION_Compliance, savedOptionCompliance);
		COMPLETION_PROJECT.setOptions(options);	
	}
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	void foo() {\n" +
			"   for (int i = 0; i < 10; i++)\n" +
			"       ret\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ret";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"return[KEYWORD]{return, null, null, return, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	void foo() {\n" +
			"   for (int i = 0; i < 10; i++)\n" +
			"       bre\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "bre";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"break[KEYWORD]{break, null, null, break, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	void foo() {\n" +
			"   for (int i = 0; i < 10; i++)\n" +
			"       cont\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "cont";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"continue[KEYWORD]{continue, null, null, continue, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493e() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	int foo(int p) {\n" +
			"       if (p == 0)\n" +
			"           return 0;\n" +
			"       else\n" +
			"           ret\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "ret";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"return[KEYWORD]{return, null, null, return, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493f() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	int foo(int p) {\n" +
			"       if (p == 0)\n" +
			"           return 0;\n" +
			"       els\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "els";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"else[KEYWORD]{else, null, null, else, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493g() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	int foo(int p) {\n" +
			"       els\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "els";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493h() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	int foo(int p) {\n" +
			"       if (p == 0)\n" +
			"           return 0;\n" +
			"       else\n" +
			"           els\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "els";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for loop without block
public void test269493i() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test269493 {\n" +
			"	int foo(int p) {\n" +
			"       if (p == 0) {\n" +
			"           return 0;\n" +
			"       }\n" +
			"       else\n" +
			"           els\n" +
			"	}\n" +
			"}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "els";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=253008, prefer boolean proposal inside if(), while() etc
public void test253008() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"       if (methodR) {\n" +
			"           return 0;\n" +
			"       }\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID  + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test253008b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"       while (methodR) {\n" +
			"           return 0;\n" +
			"       }\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID  + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test253008c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"   do { \n" +
			"   }  while (methodR);\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID  + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test253008d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"   for (int i = 0; methodR; i++) {\n" +
			"   }\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_VOID + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_EXACT_EXPECTED_TYPE + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test253008e() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"   for (methodR; true;) {\n" +
			"   }\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test253008f() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test269493.java",
			"package test;" +
			"public class Test253008 {\n" +
			"	boolean methodReturningBoolean() { return true; }\n" +
			"   void methodReturningBlah() { return; }\n" +
			"	int foo(int p) {\n" +
			"   for (int i = 0; true; methodR) {\n" +
			"   }\n" +
			"	}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "methodR";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"methodReturningBlah[METHOD_REF]{methodReturningBlah(), Ltest.Test253008;, ()V, methodReturningBlah, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"methodReturningBoolean[METHOD_REF]{methodReturningBoolean(), Ltest.Test253008;, ()Z, methodReturningBoolean, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

public void test201762() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[8];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test201762.java",
			"import test.ControlAdapter;\n" +
			"import test.Label;\n" +
			"import test.PaintEvent;\n" +
			"import test.PaintListener;\n" +
			"import test.SWT;\n" +
			"import test.Shell;\n" +
			"\n" +
			"public class Test201762 {\n" +
			"\n" +
			"    void main(Shell shell) {\n" +
			"\n" +
			"        final Label label= new Label(shell, SWT.WRAP);\n" +
			"        label.addPaintListener(new PaintListener() {\n" +
			"            public void paintControl(PaintEvent e) {\n" +
			"                e.gc.setLineCap(SWT.CAP_); // content assist after CAP_\n" +
			"            }\n" +
			"        });\n" +
			"\n" +
			"        shell.addControlListener(new ControlAdapter() { });\n" +
			"\n" +
			"        while (!shell.isDisposed()) { }\n" +
			"    }\n" +
			"}\n");
	
	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/AnObject.java",
			"package test;\n" +
			"public class AnObject {\n" +
			"	public void setLineCap(int capZz) {}\n" +
			"}\n");
	
	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/ControlAdapter.java",
			"package test;\n" +
			"public class ControlAdapter {\n" +
			"}\n");
	
	this.workingCopies[3] = getWorkingCopy(
			"/Completion/src/test/Label.java",
			"package test;\n" +
			"public class Label {\n" +
			"	public Label(Shell shell, int wrap) {\n" +
			"	}\n" +
			"	public void addPaintListener(PaintListener paintListener) {\n" +
			"	}\n" +
			"}\n");
	
	this.workingCopies[4] = getWorkingCopy(
			"/Completion/src/test/Label.java",
			"package test;\n" +
			"public class PaintEvent {\n" +
			"	public AnObject gc;\n" +
			"}\n");
	
	this.workingCopies[5] = getWorkingCopy(
			"/Completion/src/test/PaintListener.java",
			"package test;\n" +
			"public interface PaintListener {\n" +
			"}\n");
	
	this.workingCopies[6] = getWorkingCopy(
			"/Completion/src/test/Shell.java",
			"package test;\n" +
			"public class Shell {\n" +
			"	public boolean isDisposed() {\n" +
			"		return false;\n" +
			"	}\n" +
			"	public void addControlListener(ControlAdapter controlAdapter) {\n" +
			"	}\n" +
			"}\n");
	
	this.workingCopies[7] = getWorkingCopy(
			"/Completion/src/test/SWT.java",
			"package test;\n" +
			"public interface SWT {\n" +
			"	int WRAP = 0;\n" +
			"	int CAP_ZZ = 0;\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "SWT.CAP_";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"CAP_ZZ[FIELD_REF]{CAP_ZZ, Ltest.SWT;, I, CAP_ZZ, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_EXACT_EXPECTED_TYPE + R_NON_INHERITED + R_NON_RESTRICTED) + "}",
			requestor.getResults());
}


/**
 * An inner/member class should not be offered as completion suggestion.
 * @throws JavaModelException
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=270437"
 */
public void test270437a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270437a.java",
			"package test;" +
			"public class Test270437a extends Test270437a. {\n" +
			"	public class Inner {}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Test270437a.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	// In the absence of the fix, the results would be as follows, which is wrong:
	//"Test270437a.Inner[TYPE_REF]{Inner, test, Ltest.Test270437a$Inner;, null, 39}",
	assertResults(
			"", //Empty!
			requestor.getResults());
}

/**
 * An inner/member interface should not be offered as completion suggestion.
 * @throws JavaModelException
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=270437"
 */
public void test270437b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270437b.java",
			"package test;" +
			"public class Test270437b implements Test270437b. {\n" +
			"	public interface Inner {}\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Test270437b.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	// In the absence of the fix, the results would be as follows, which is wrong:
	//"Test270437b.Inner[TYPE_REF]{Inner, test, Ltest.Test270437b$Inner;, null, 39}",
	assertResults(
			"", //Empty!
			requestor.getResults());
}

/**
 * An inner/member interface should not be offered as completion suggestion.
 * @throws JavaModelException
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=270437"
 */
public void test270437c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270437c.java",
			"package test;" +
			"class X extends X.MyClass1. {\n" +
		    "    public class MyClass1 {\n" +
		    "            public class MyClass2 {\n" +
		    "            }\n" +
		    "    }\n" +
			"}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "X.MyClass1.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	// In the absence of the fix, the results would be as follows, which is wrong:
	//"X.MyClass1.MyClass2[TYPE_REF]{MyClass2, test, Ltest.X$MyClass1$MyClass2;, null, 39}",
	assertResults(
			"", //Empty!
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=270436
public void test270436a() throws JavaModelException {
	// This test is to ensure that an interface is not offered as a choice when expecting a class.
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270436a.java",
			"package test;\n" +
			"public final class TestClass {}\n" +
			"interface TestInterface {}\n" +
			"class Subclass extends test.Test {}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.Test";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	// In the absence of the fix, it would have been:
	// TestInterface[TYPE_REF]{TestInterface, test, Ltest.TestInterface;, null, 19}
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=270436
public void test270436b() throws JavaModelException {
	// This test is to ensure that itself is not offered as choice during completion.
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270436b.java",
			"package test;\n" +
			"public final class TestClass {}\n" +
			"interface TestInterface {}\n" +
			"class Subclass extends test.Sub {}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.Sub";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	// In the absence of the fix, it would have been:
	// Subclass[TYPE_REF]{Subclass, test, Ltest.Subclass;, null, 39}
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=270436
public void test270436c() throws JavaModelException {
	// This test is to ensure that a class is not offered as a choice when expecting an interface.
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test270436c.java",
			"package test;\n" +
			"public final class TestClass {}\n" +
			"interface TestInterface {}\n" +
			"class Subclass {}\n" +
			"interface Subinterface extends test.");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			// In the absence of this fix, it will have these additional ones as well.
			//"Subclass[TYPE_REF]{Subclass, test, Ltest.Subclass;, null, 19}\n" +
			//"TestClass[TYPE_REF]{TestClass, test, Ltest.TestClass;, null, 19}\n" +
			"TestInterface[TYPE_REF]{TestInterface, test, Ltest.TestInterface;, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_RESTRICTED + R_INTERFACE)+ "}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276526
public void test276526a() throws JavaModelException {
	// This test is to ensure that an existing super interface is not offered as a completion choice,
	// when all of them are in the same compilation unit.
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test276526a.java",
			"package test;\n" +
			"interface IFoo {}\n" +
			"interface IBar {}\n" +
			"class Foo implements IFoo, test. {}\n");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	// In the absence of the fix, it would have been:
	// "IBar[TYPE_REF]{IBar, test, Ltest.IBar;, null, 44}\n" +
	// "IFoo[TYPE_REF]{IFoo, test, Ltest.IFoo;, null, 44}"
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			"IBar[TYPE_REF]{IBar, test, Ltest.IBar;, null, 44}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276526
public void test276526b() throws JavaModelException {
	// This test is to ensure that an existing super interface is not offered as a completion choice,
	// when they are in different compilation units.
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/test/Test276526b1.java",
			"package test;\n" +
			"public class TestClazz implements Interface1, test. {}");
			
	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/test/Test276526b2.java",
			"package test;\n" +
			"public interface Interface1 {}");
			
	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/test/Test276526b3.java",
			"package test;\n" +
			"public interface Interface2 {}");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "test.";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			// In the absence of the fix, it would also complete to Interface1:
			//"Interface1[TYPE_REF]{Interface1, test, Ltest.Interface1;, null, 44}\n"+
			"Interface2[TYPE_REF]{Interface2, test, Ltest.Interface2;, null, 44}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276526
public void test276526c() throws JavaModelException {
	// This test is to ensure that an existing super interface is not offered as a completion choice,
	// when they are in different compilation units and nested-types.
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/p/Enclosing.java",
			"package p;\n" +
			"public class Enclosing {\n" +
			"	public interface Interface1 {}\n" +
			"	public interface Interface2 {}\n" +
			"}\n");
			
	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/TestClazz.java",
			"public class TestClazz implements p.Enclosing.Interface1, Interf {}");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[1].getSource();
	String completeBehind = "Interf";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[1].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			// In the absence of the fix, it would suggest both p.Enclosing.Interface2, and
			// p.Enclosing.Interface1 while it should have suppressed the latter which is in use.
			"Enclosing.Interface2[TYPE_REF]{p.Enclosing.Interface2, p, Lp.Enclosing$Interface2;, null, 44}",
			requestor.getResults());
}

//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276526
public void test276526d() throws JavaModelException {
	// This test is to ensure that an existing super interface is not offered as a completion choice,
	// when they are in different compilation units and combination of nested and top-level types.
	this.workingCopies = new ICompilationUnit[3];
	this.workingCopies[0] = getWorkingCopy(
			"/Completion/src/p/Eclosing.java",
			"package p;\n" +
			"public class Enclosing {\n" +
			"	public interface Interface1 {}\n" +
			"	public interface Interface2 {}\n" +
			"}\n");
			
	this.workingCopies[1] = getWorkingCopy(
			"/Completion/src/p/Interface1.java",
			"package p;\n" +
			"public interface Interface1 {}");
			
	this.workingCopies[2] = getWorkingCopy(
			"/Completion/src/TestClazz.java",
			"public class TestClazz implements p.Interface1, Interf {}");
			
	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2();
	String str = this.workingCopies[2].getSource();
	String completeBehind = "Interf";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();

	this.workingCopies[2].codeComplete(cursorLocation, requestor, this.wcOwner);
	assertResults(
			// In the absence of the fix, it would suggest only p.Enclosing.Interface2, as it
			// was wrongly suppressing p.Enclosing.Interface1 confusing it with p.Interface1.
			"Enclosing.Interface1[TYPE_REF]{p.Enclosing.Interface1, p, Lp.Enclosing$Interface1;, null, 44}\n" +
			"Enclosing.Interface2[TYPE_REF]{p.Enclosing.Interface2, p, Lp.Enclosing$Interface2;, null, 44}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
// To verify that auto complete works after instance of expression when content assist is requested on a field
// Code assist requested in a local variable declaration statement
public void testBug287939a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"		if (this.a instanceof CompletionAfterInstanceOf) {\n" +
		"			int i =  this.a.r\n" +
		"       	int j = 0;\n" +
		"       	int k = 2;\n" +
		"       	int p = 12;\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("this.a.r");
	int end2 = start2 + "this.a.r".length();
	int start3 = str.lastIndexOf("this.a.");
	int end3 = start3 + "this.a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)this.a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
// To verify that auto complete works after instance of expression when content assist is requested on a field
// Code assist requested in an assignment statement
public void testBug287939b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	public Object a;\n" +
		"	void bar(){\n" +
		"       int i;\n" +
		"		if (this.a instanceof CompletionAfterInstanceOf) {\n" +
		"			i =  this.a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "this.a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("this.a.r");
	int end2 = start2 + "this.a.r".length();
	int start3 = str.lastIndexOf("this.a.");
	int end3 = start3 + "this.a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)this.a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
// To verify that auto complete works after instance of expression when content assist is requested on a local variable
// Code assist requested in an assignment statement
public void testBug287939c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object a){\n" +
		"       int i;\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {" +
		"				i =  a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("a.r");
	int end2 = start2 + "a.r".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
// To verify that auto complete works after instance of expression when content assist is requested on a local variable
// Code assist requested in a local variable declaration statement
public void testBug287939d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object a){\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {" +
		"				int i =  a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("a.r");
	int end2 = start2 + "a.r".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"	void foo() {\n" +
		"		sup\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "sup";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("sup");
	int end1 = start1 + "sup".length();
	
	assertResults(
			"super[KEYWORD]{super, null, null, super, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	void foo() {\n" +
		"		sup\n" +
		"	}\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "sup";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("sup");
	int end1 = start1 + "sup".length();
	
	assertResults(
			"super[KEYWORD]{super, null, null, super, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"	{" +
		"		sup" +
		"	}" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "sup";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("sup");
	int end1 = start1 + "sup".length();
	
	assertResults(
			"super[KEYWORD]{super, null, null, super, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"	Test() {" +
		"		sup" +
		"	}" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "sup";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("sup");
	int end1 = start1 + "sup".length();
	
	assertResults(
			"super[KEYWORD]{super, null, null, super, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}\n" +
			"super[METHOD_REF<CONSTRUCTOR>]{super(), Ljava.lang.Object;, ()V, super, null, replace[" + start1 + ", " + end1 + "], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634e() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"	{" +
		"		thi" +
		"	}" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "thi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("thi");
	int end1 = start1 + "thi".length();
	
	assertResults(
			"this[KEYWORD]{this, null, null, this, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
public void testBug202634f() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;\n" +
		"public class Test {\n" +
		"	public static void bar(){" +
		"		System.out.println(\"bar\");\n" +
		"	}\n" +
		"	Test() {" +
		"		thi" +
		"	}" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "thi";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	int start1 = str.lastIndexOf("thi");
	int end1 = start1 + "thi".length();
	
	assertResults(
			"this[KEYWORD]{this, null, null, this, null, replace[" + start1 + ", " + end1 +"], token[" + start1 + ", " + end1 + "], " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE+ R_NON_RESTRICTED) + "}",
			requestor.getResults());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=307337
public void testBug307337() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/TestBug307337.java",
		"package test;\n" +
		"public class TestBug307337 {\n" +
		"    Object obj = new Object() {\n" +
		"    TestBug307337\n" + 	
		"     };\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	requestor.setRequireExtendedContext(true);
	requestor.setComputeEnclosingElement(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "TestBug307337";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);
	
	assertResults(
			"expectedTypesSignatures=null\n" +
			"expectedTypesKeys=null\n" +
			"enclosingElement=<anonymous #1> {key=Ltest/TestBug307337$64;} [in obj [in TestBug307337 [in [Working copy] TestBug307337.java [in test [in src [in Completion]]]]]]",
			requestor.getContext());
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=292087
// To verify that anonymous class in array initializer doesnt cause
// grief to content assist
public void testBug292087() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"public class Try extends Thread{\n" +
		"	public Runnable member[] = { new Runnable (){\n" +
		"		public void run() {}\n" +
		"		}\n" +
		"	};\n" +
		"	Tr\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Tr";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			// without the fix no proposals obtained.
			"Tr[POTENTIAL_METHOD_DECLARATION]{Tr, Ltest.Try;, ()V, Tr, null, 14}\n" +
			"transient[KEYWORD]{transient, null, null, transient, null, 14}\n" +
			"Try[TYPE_REF]{Try, test, Ltest.Try;, null, null, 27}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=249704
public void testBug249704() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"import java.util.Arrays;\n" +
		"public class Try {\n" +
		"	Object obj = new Object() {\n" +
		"		public void method() {\n" +
		"			Object obj = new Object() {\n" +
		"				int a = 1;\n" +
		"				public void anotherMethod() {\n" +
		"					try {}\n" +
		"					catch (Throwable e) {}\n" +
		"					Arrays.sort(new String[]{\"\"});\n" +
		"				}\n" +
		"			};\n" +
		"			e\n" +
		"		}\n" +
		"	};\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "e";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			// without the fix no proposals obtained.
			"Error[TYPE_REF]{Error, java.lang, Ljava.lang.Error;, null, null, 17}\n" +
			"Exception[TYPE_REF]{Exception, java.lang, Ljava.lang.Exception;, null, null, 17}\n" +
			"equals[METHOD_REF]{Try.this.equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), 24}\n" +
			"equals[METHOD_REF]{equals(), Ljava.lang.Object;, (Ljava.lang.Object;)Z, equals, (obj), 27}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=244820
// To verify that autocast works correctly even when the compilation unit
// has some compilation errors.
public void testBug244820() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;" +
		"class MyString {\n" +
		"	public String toWelcome() {\n" +
		"		return \"welcome\";\n" +
		"	}\n" +
		"}\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	void foo() {\n" +
		"	    Object chars= null;\n" +
		"	    Object ch = null;\n" +
		"		String lower = null;\n" +
		"       if (ch instanceof MyString) {\n" +
		"			ch.t; \n" +
		"		}\n" +
		"       if (ch instanceof MyString) {\n" +
		"			return ch.t; \n" +
		"		}\n" +
		"       if (chars instanceof MyString) {\n" +
		"			lower = chars.to; \n" +
		"		}\n" +
		"       if (ch instanceof MyString) {\n" +
		"			String low = ch.t; \n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, true, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "lower = chars.to";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("to") + "".length();
	int end1 = start1 + "to".length();
	int start2 = str.lastIndexOf("chars.to");
	int end2 = start2 + "chars.to".length();
	int start3 = str.lastIndexOf("chars.");
	int end3 = start3 + "chars".length();
	
	assertResults(
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, null, null, toString, null, replace[" + start1 + ", " + end1 + "], token[" + start1 + ", " + end1 +"], " + relevance1 + "}\n" +
			"toWelcome[METHOD_REF_WITH_CASTED_RECEIVER]{((MyString)chars).toWelcome(), Ltest.MyString;, ()Ljava.lang.String;, Ltest.MyString;, null, null, toWelcome, null, replace[" + start2 +", " + end2 + "], token[" + start1 + ", " + end1 + "], receiver[" + start3 + ", " + end3 + "], " + relevance1 + "}",
			requestor.getResults());
}


// https://bugs.eclipse.org/bugs/show_bug.cgi?id=308980
public void testBug308980a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"import java.util.Arrays;\n" +
		"public class Try {\n" +
		"	public static final AClass a1 = new JustTry(\n" +
		"								new byte[][] {\n" +
		"								{0x00,0x3C},\n" +
		"								{0x04,0x2C}}) {\n" +
		"									int justReturn (int a){\n" +
		"										return a;\n" +
		"									}\n" +
		"								};\n" +
		"   public static final AC" +
		"}\n" +
		"class AClass{\n" +
		"	public byte[][] field1;\n" +
		"	public AClass(byte[][] byteArray) {\n" +
		"		field1 = byteArray;\n" +
		"	}\n" +
		"}\n" +
		"abstract class JustTry extends AClass {\n" +
		"	public byte[][] field1;\n" +
		"	public JustTry (byte[][] byteArray){\n" +
		"		field1 = byteArray;\n" +
		"	}\n" +
		"	abstract int justReturn(int a);\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "public static final AC";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"AClass[TYPE_REF]{AClass, test, Ltest.AClass;, null, null, 27}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=308980
public void testBug308980b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"import java.util.Arrays;\n" +
		"public class Try {\n" +
		"	public static final test.AClass a1 = new JustTry(\n" +
		"								new byte[][] {\n" +
		"								{0x00,0x3C},\n" +
		"								{0x04,0x2C}}) {\n" +
		"									int justReturn (int a){\n" +
		"										return a;\n" +
		"									}\n" +
		"								};\n" +
		"   public static final AC" +
		"}\n" +
		"class AClass{\n" +
		"	public byte[][] field1;\n" +
		"	public AClass(byte[][] byteArray) {\n" +
		"		field1 = byteArray;\n" +
		"	}\n" +
		"}\n" +
		"abstract class JustTry extends AClass {\n" +
		"	public byte[][] field1;\n" +
		"	public JustTry (byte[][] byteArray){\n" +
		"		field1 = byteArray;\n" +
		"	}\n" +
		"	abstract int justReturn(int a);\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "public static final AC";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"AClass[TYPE_REF]{AClass, test, Ltest.AClass;, null, null, 27}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=267091
// To verify that we get interface proposals after 'implements'
public void testBug267091a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"interface In{}\n" +
		"interface Inn{\n" +
		"	interface Inn2{}\n" +
		"}\n" +
		"class ABC {\n" +
		"	interface ABCInterface;\n" +
		"}\n" +
		"public class Try implements {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Try implements ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			// without the fix no proposals obtained.
			"Inn.Inn2[TYPE_REF]{test.Inn.Inn2, test, Ltest.Inn$Inn2;, null, null, 44}\n" +
			"ABC.ABCInterface[TYPE_REF]{ABCInterface, test, Ltest.ABC$ABCInterface;, null, null, 47}\n" +
			"In[TYPE_REF]{In, test, Ltest.In;, null, null, 47}\n" +
			"Inn[TYPE_REF]{Inn, test, Ltest.Inn;, null, null, 47}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=267091
// To verify that we get type proposals after 'extends'
public void testBug267091b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Try.java",
		"package test;\n" +
		"class In{}\n" +
		"class Inn{\n" +
		"	class Inn2{\n" +
		"		class Inn3{\n" +
		"		}\n" +
		"	}\n" +
		"}\n" +
		"class ABC extends Inn{}\n" +
		"public class Try extends {\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "Try extends ";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			// without the fix no proposals obtained.
			"Inn.Inn2[TYPE_REF]{test.Inn.Inn2, test, Ltest.Inn$Inn2;, null, null, 44}\n" +
			"Inn.Inn2.Inn3[TYPE_REF]{test.Inn.Inn2.Inn3, test, Ltest.Inn$Inn2$Inn3;, null, null, 44}\n" +
			"ABC[TYPE_REF]{ABC, test, Ltest.ABC;, null, null, 47}\n" +
			"In[TYPE_REF]{In, test, Ltest.In;, null, null, 47}\n" +
			"Inn[TYPE_REF]{Inn, test, Ltest.Inn;, null, null, 47}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=261534
// To verify that autocast works correctly even when instanceof expression
// and completion node are in the same binary expression, related by &&
public void testBug261534a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;" +
		"class MyString {\n" +
		"	public String toWelcome() {\n" +
		"		return \"welcome\";\n" +
		"	}\n" +
		"}\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	void foo() {\n" +
		"	Object chars= null;\n" +
		"       if (chars instanceof MyString && chars.to) {\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, true, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "chars instanceof MyString && chars.to";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("to") + "".length();
	int end1 = start1 + "to".length();
	int start2 = str.lastIndexOf("chars.to");
	int end2 = start2 + "chars.to".length();
	int start3 = str.lastIndexOf("chars.");
	int end3 = start3 + "chars".length();
	
	assertResults(
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, null, null, toString, null, replace[" + start1 + ", " + end1 + "], token[" + start1 + ", " + end1 +"], " + relevance1 + "}\n" +
			"toWelcome[METHOD_REF_WITH_CASTED_RECEIVER]{((MyString)chars).toWelcome(), Ltest.MyString;, ()Ljava.lang.String;, Ltest.MyString;, null, null, toWelcome, null, replace[" + start2 +", " + end2 + "], token[" + start1 + ", " + end1 + "], receiver[" + start3 + ", " + end3 + "], " + relevance1 + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=261534
// Negative test - To verify that proposals from casted receiver are not obtained
// when completion node is in an OR_OR_Expression along with an instanceof exp.
public void testBug261534b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;" +
		"class MyString {\n" +
		"	public String toWelcome() {\n" +
		"		return \"welcome\";\n" +
		"	}\n" +
		"}\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	void foo() {\n" +
		"	Object chars= null;\n" +
		"       if (chars instanceof MyString || chars.to) {\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, true, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "chars instanceof MyString || chars.to";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("to") + "".length();
	int end1 = start1 + "to".length();
	
	assertResults(
			"toString[METHOD_REF]{toString(), Ljava.lang.Object;, ()Ljava.lang.String;, null, null, toString, null, replace[" + start1 + ", " + end1 + "], token[" + start1 + ", " + end1 +"], " + relevance1 + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=310427
// To verify that we don't get proposals for fields that have not yet been declared
// inside a field declaration statement
public void testBug310427a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"       int myVar1 = 1;\n" +
		"		int myVar2 = 1;\n" +
		"		int myVar3 = myVar;\n" +
		"       int myVar4 = 1;\n" +
		"		int myVar5 = 1;\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "int myVar3 = myVar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"myVar1[FIELD_REF]{myVar1, Ltest.Test;, I, myVar1, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE) + "}\n" +
			"myVar2[FIELD_REF]{myVar2, Ltest.Test;, I, myVar2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=310427
// To verify that we don't get proposals for the local variable in whose declaration
// content assist is being invoked
public void testBug310427b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/Test.java",
		"package test;"+
		"public class Test {\n" +
		"	public static void main() {\n" +
		"		int myVar1 = 1;\n" +
		"		int myVar2 = myVar;\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "int myVar2 = myVar";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"myVar1[LOCAL_VARIABLE_REF]{myVar1, null, I, myVar1, null, 57}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
// To verify that array types aren't proposed inside case, and also
// that finals have a higher priority in suggestions inside case expressions.
public void testBug195346a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterCase2.java",
		"package test;\n" +
		"public class CompletionAfterCase2 {\n" +
		"	static char[] AN_ARRAY = new char[10];\n" +
		"	static int AN_INT_VALUE = 0;\n" +
		"	static final int AN_INT_VALUE2 = 0;\n" +
		"	static final char[] AN_ARRAY2 = {'a','b'};\n" +
		"	static final int[] AN_INT_ARRAY = null;\n" +
		"	static final Object[] ANOTHER_ARRAY = null;\n" +
		"	void foo(int i, final int [] AN_ARRAY_PARAM){\n" +
		"		final int AN_INT_VAR = 1;\n" +
		"		final int[] AN_ARRAY_VAR = {1};\n" +
		"		int AN_INT_VAR2 = 1;\n" +
		"		switch(i) {\n" +
		"			case AN\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "AN";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"AN_INT_VALUE[FIELD_REF]{AN_INT_VALUE, Ltest.CompletionAfterCase2;, I, AN_INT_VALUE, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE) + "}\n" +
			"AN_INT_VAR2[LOCAL_VARIABLE_REF]{AN_INT_VAR2, null, I, AN_INT_VAR2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE) + "}\n" +
			"AN_INT_VALUE2[FIELD_REF]{AN_INT_VALUE2, Ltest.CompletionAfterCase2;, I, AN_INT_VALUE2, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE + R_FINAL) + "}\n" +
			"AN_INT_VAR[LOCAL_VARIABLE_REF]{AN_INT_VAR, null, I, AN_INT_VAR, null, " + (R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE + R_FINAL) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
// To verify that array types aren't proposed inside case, and also
// that finals have a higher priority in suggestions inside case expressions.
public void testBug195346b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterCase2.java",
		"package test;\n" +
		"public class CompletionAfterCase2 {\n" +
		"	class AN_INNER_CLASS {\n" +
		"		static final int abc = 1;\n" +
		"	}\n" +
		"	static char[] AN_ARRAY = new char[10];\n" +
		"	static int AN_INT_VALUE = 0;\n" +
		"	static final int AN_INT_VALUE2 = 0;\n" +
		"	static final char[] AN_ARRAY2 = {'a','b'};\n" +
		"	static final int[] AN_INT_ARRAY = null;\n" +
		"	static final Object[] ANOTHER_ARRAY = null;\n" +
		"	void foo(int i, final int [] AN_ARRAY_PARAM){\n" +
		"		final int AN_INT_VAR = 1;\n" +
		"		final int[] AN_ARRAY_VAR = {1};\n" +
		"		int AN_INT_VAR2 = 1;\n" +
		"		switch(i) {\n" +
		"			case CompletionAfterCase2.AN\n" +
		"		}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true);
	String str = this.workingCopies[0].getSource();
	String completeBehind = "AN";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	assertResults(
			"CompletionAfterCase2.AN_INNER_CLASS[TYPE_REF]{AN_INNER_CLASS, test, Ltest.CompletionAfterCase2$AN_INNER_CLASS;, null, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED) + "}\n" +
			"AN_INT_VALUE[FIELD_REF]{AN_INT_VALUE, Ltest.CompletionAfterCase2;, I, AN_INT_VALUE, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE) + "}\n" +
			"AN_INT_VALUE2[FIELD_REF]{AN_INT_VALUE2, Ltest.CompletionAfterCase2;, I, AN_INT_VALUE2, null, " + (R_DEFAULT + R_INTERESTING + R_CASE + R_UNQUALIFIED + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE + R_FINAL) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
// To verify that autocast works correctly after an instanceof even if there are other expressions in between
// instanceof and the place where code assist is requested.
public void testBug304006a() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int foo() { return 1; }\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object a){\n" +
		"       int i = 1;\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {" +
		"			if (i == 1)\n" +
		"				i =  a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("a.r");
	int end2 = start2 + "a.r".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
// To verify that autocast works correctly after an instanceof even if there are other expressions in between
// instanceof and the place where code assist is requested.
public void testBug304006b() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int foo() { return 1; }\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object a){\n" +
		"       int i = 1;\n" +
		"		if (a instanceof CompletionAfterInstanceOf) {" +
		"			if (i == 1)\n" +
		"				a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("a.r");
	int end2 = start2 + "a.r".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
// To verify that autocast works correctly even when the completion node
// is preceeded by a number of unrelated instanceof expressions
public void testBug304006c() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int foo() { return 1; }\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object abc, Object xyz){\n" +
		"       int i = 1, j;\n" +
		"		if(i == 1)\n" +
		"			if (abc instanceof CompletionAfterInstanceOf)\n" +
		"				if(xyz instanceof CompletionAfterInstanceOf){\n" +
		"					j = 1;\n" +
		"					if(j == 1)\n" +
		"						i = abc.r \n" +
		"				}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "abc.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("abc.r");
	int end2 = start2 + "abc.r".length();
	int start3 = str.lastIndexOf("abc.");
	int end3 = start3 + "abc".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)abc).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
// To verify that autocast works correctly even when the completion node is preceeded by a number 
// of unrelated instanceof expressions. This case has errors in compilation unit. 
public void testBug304006d() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int foo() { return 1; }\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object abc, Object xyz){\n" +
		"       int i = 1, j;\n" +
		"		j = \n" +
		"		if(i == 1)\n" +
		"			if (abc instanceof CompletionAfterInstanceOf)\n" +
		"				if(xyz instanceof CompletionAfterInstanceOf){\n" +
		"					j = 1;\n" +
		"					if(j == 1)\n" +
		"						xyz.r \n" +
		"				}\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "xyz.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("xyz.r");
	int end2 = start2 + "xyz.r".length();
	int start3 = str.lastIndexOf("xyz.");
	int end3 = start3 + "xyz".length();
	
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)xyz).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
// To verify that autocast works correctly after an instanceof even if there are other expressions in between
// instanceof and the place where code assist is requested. This is a case with errors in the compilation unit.
public void testBug304006e() throws JavaModelException {
	this.workingCopies = new ICompilationUnit[1];
	this.workingCopies[0] = getWorkingCopy(
		"/Completion/src/test/CompletionAfterInstanceOf.java",
		"package test;\n" +
		"public class CompletionAfterInstanceOf {\n" +
		"	public int foo() { return 1; }\n" +
		"	public int returnZero(){ return 0;}\n" +
		"	void bar(Object a){\n" +
		"       int i = 1;\n" +
		"       int i = \n" +
		"		if (a instanceof CompletionAfterInstanceOf) {" +
		"			if (i == 1)\n" +
		"				i =  a.r\n" +
		"	}\n" +
		"}\n");

	CompletionTestsRequestor2 requestor = new CompletionTestsRequestor2(true, false, true, true, true, true);
	requestor.allowAllRequiredProposals();
	String str = this.workingCopies[0].getSource();
	String completeBehind = "a.r";
	int cursorLocation = str.lastIndexOf(completeBehind) + completeBehind.length();
	this.workingCopies[0].codeComplete(cursorLocation, requestor, this.wcOwner);

	int relevance1 = R_DEFAULT + R_RESOLVED + R_INTERESTING + R_CASE + R_NON_STATIC + R_NON_RESTRICTED + R_EXACT_EXPECTED_TYPE;
	int start1 = str.lastIndexOf("r") + "".length();
	int end1 = start1 + "r".length();
	int start2 = str.lastIndexOf("a.r");
	int end2 = start2 + "a.r".length();
	int start3 = str.lastIndexOf("a.");
	int end3 = start3 + "a".length();
	
	assertResults(
			"expectedTypesSignatures={I}\n" +
			"expectedTypesKeys={I}",
			requestor.getContext());
	assertResults(
			"returnZero[METHOD_REF_WITH_CASTED_RECEIVER]{((CompletionAfterInstanceOf)a).returnZero(), Ltest.CompletionAfterInstanceOf;, ()I, Ltest.CompletionAfterInstanceOf;, returnZero, null, replace["+start2+", "+end2+"], token["+start1+", "+end1+"], receiver["+start3+", "+end3+"], " + (relevance1) + "}",
			requestor.getResults());
}
}
