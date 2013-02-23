package org.hackerleague.extractor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @description A simple tool (Eclipse plugin) that extracts every hard-coded
 *  string from Android .java files to strings.xml with just one click. 
 *  To do it now, you have to search for every hard-coded word on your own.
 *  Created during 5 hours, so code needs to be polished. 
 *  
 * @author Adam Stelmaszczyk, Michał Karpiuk
 * @date 22/23.02.2013
 * @see https://code.google.com/p/android-string-extractor-svn
 * @see https://www.hackerleague.org/hackathons/name-collision/hacks/android-string-extractor
 * 
 * TODO: Further development ideas.
 * 1. Extracting from all the source files with one click on the project, not from only one selected.
 * 2. Updating existing .java files instead of writing to some external files.
 * 3. Creating better string identifiers (STRING_IDs).
 */
public class ConvertHandler extends AbstractHandler
{
	private QualifiedName path = new QualifiedName("html", "path");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Shell shell = HandlerUtil.getActiveShell(event);
		ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) sel;

		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof ICompilationUnit)
		{
			createOutput(shell, firstElement);

		}
		else
		{
			MessageDialog.openInformation(shell, "Info", "Please select a Java source file");
		}
		return null;
	}

	private void createOutput(Shell shell, Object firstElement)
	{
		String directory;
		ICompilationUnit cu = (ICompilationUnit) firstElement;
		IResource res = cu.getResource();
		boolean newDirectory = true;
		directory = getPersistentProperty(res, path);

		if (directory != null && directory.length() > 0)
		{
			newDirectory = !(MessageDialog.openQuestion(shell, "Question", "Use the previous output directory?"));
		}
		if (newDirectory)
		{
			DirectoryDialog fileDialog = new DirectoryDialog(shell);
			directory = fileDialog.open();

		}
		if (directory != null && directory.length() > 0)
		{
			setPersistentProperty(res, path, directory);
			write(directory, cu, res.getName());
		}
	}

	protected String getPersistentProperty(IResource res, QualifiedName qn)
	{
		try
		{
			return res.getPersistentProperty(qn);
		}
		catch (CoreException e)
		{
			return "";
		}
	}

	protected void setPersistentProperty(IResource res, QualifiedName qn, String value)
	{
		try
		{
			res.setPersistentProperty(qn, value);
		}
		catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private void write(String dir, ICompilationUnit unit, String string)
	{
		try
		{
			final String content = unit.getSource();
			final String corrected = content.replaceAll("\"(.*?)\"", "STRING_ID");

			String test = unit.getCorrespondingResource().getName();
			String[] name = test.split("\\.");
			String htmlFile = dir + "\\" + name[0] + "Extracted.java";
			String stringsFile = dir + "\\strings.xml";

			Pattern pattern = Pattern.compile("\"(.*?)\"");
			Matcher matcher = pattern.matcher(content);
			FileWriter outputStrings = new FileWriter(stringsFile);
			BufferedWriter stringsWriter = new BufferedWriter(outputStrings);

			int counter = 0;
			while (matcher.find())
			{
				counter++;
				matcher.group();
				stringsWriter.write("<string name=\"STRING_ID_" + counter + "\">" + matcher.group() + "</string>\n");
			}
			stringsWriter.flush();

			FileWriter output = new FileWriter(htmlFile);
			BufferedWriter writer = new BufferedWriter(output);
			writer.write(corrected);
			writer.flush();
		}
		catch (JavaModelException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}