package org.apache.bookkeeper.examTests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.test.BookKeeperClusterTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.LedgerEntry;
import org.apache.bookkeeper.client.LedgerHandle;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

@RunWith(value= Parameterized.class)
public class LedgerHandleTest extends BookKeeperClusterTestCase{

	int numberOfEntries;
	long ledgerId;
	byte[] password;
	BookKeeper bkClient;
	LedgerHandle lh;

	String expected;
	byte[] data;
	int offset;
	int length;
	boolean isClosed;
	public void configure(String expected, String data, int offset, int length, String status) throws Exception {
		this.setUp();

		//Environment SETUP:
		numberOfEntries = 1;
		password = "some-password".getBytes();
		bkClient = this.bkc;
		lh = bkClient.createLedger(BookKeeper.DigestType.MAC, password);
		ledgerId = lh.getId();

		System.out.println(data);
		//Test parameter SETUP:
		this.expected = expected;
		if(data.equals("not_null"))
		{
			this.data = new byte[20];
			new Random().nextBytes(this.data);
		}
		if(status.equals("closed"))
			isClosed=true;
		else
			isClosed=false;
		this.offset = offset;
		this.length = length;
	}
	
	public LedgerHandleTest(String expected, String data, String offset, String length, String status) throws Exception {
		super(3);
		configure(expected, data, Integer.parseInt(offset), Integer.parseInt(length), status);
	}

	@Parameterized.Parameters
	public static Collection<String[]> getTestParameters()
	{
		final int LENGTH = 20;
		return Arrays.asList(new String[][] {
			/**
 			*Category Partition Analysis:
			*byte[] data:
 			*	DummyCatergories:{null},{not_null}
 			*
			*
 			*int offset:
 			*	DummyCatergories:{null},{not_null}
 			*	NotTrivialCategories:{<0},{x€[0,data.length-1]},{>=data.length}
 			*
			*int length:
			*	DummyCatergories:{null},{not_null}
			*	NotTrivialCategories: {<0},{x€[0,data.length-offset)},{>=data.length-offset}
			*
			*	data.length := 20
			*
			*Raffinamento coverage:
			*LadgerHandle stateAdd:
			*	Categories:{closed}{opened}
			*/

			{"ok",		"not_null",	"0",	"1",	"opened"							},	//{not_null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}
			{"ok",		"not_null",	"0",	"20",	"opened"							},	//{not_null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}
			{"ok",		"not_null",	"15",	Integer.toString(LENGTH-15-1), "opened"	},	//{not_null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}
			{"ok",		"not_null",	"0",	"0",	"opened"							},	//{not_null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}


			{"null",	"null",		"0",	"0",	"opened"							},	//{null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}
			{"error",	"not_null",	"-1",	"0",	"opened"							},	//{null},	{<0},					{x€[0,data.length-offset]},	{opened}
			{"error",	"not_null",	"0",	"-1",	"opened"							},	//{null},	{x€[0,data.length-1]},	{<0},						{opened}
			{"error",	"not_null",	"21",	"0",	"opened"							}, 	//{null},	{>=data.length},		{x€[0,data.length-offset]},	{opened}
			{"error",	"not_null",	"0",	"21",	"opened"							},	//{null},	{x€[0,data.length-1]},	{>=data.length-offset},		{opened}
			{"error",	"not_null",	"15",	Integer.toString(LENGTH+15), "opened"	},	//{null},	{x€[0,data.length-1]},	{>=data.length-offset},		{opened}
			{"ok",		"not_null",	"0",	"1",	"closed"							},	//{not_null},	{x€[0,data.length-1]},	{x€[0,data.length-offset]},	{opened}


		});
	}
	@Test
	public void addAndReadEntryTest(){
		long returnedEntryId;
		LedgerEntry returnedByteArray;
		switch(expected)
		{
			case "ok":
				try {
					if(isClosed)
						lh.close();
					returnedEntryId = lh.addEntry(data,offset,length);
					assertEquals(true, returnedEntryId>=0);


					lh.readEntries(0, numberOfEntries-1);
					Enumeration<LedgerEntry> entries = lh.readEntries(0, numberOfEntries - 1);
					while(entries.hasMoreElements()) {
						returnedByteArray = entries.nextElement();
						assertEquals(true, returnedByteArray.getEntryId()==returnedEntryId);
						assertEquals(true, Arrays.equals(returnedByteArray.getEntry(),
																	Arrays.copyOfRange(data,offset,offset+length)));
					}
				} catch (InterruptedException | BKException e) {
					e.printStackTrace();
					if(e instanceof BKException && isClosed)
						return;
					Assert.fail("Fail addEntryTest:\nExpected: "+expected
							+"\ndata: "+Arrays.toString(data)
							+"\noffset: "+offset
							+"\nlength: "+length
							+"\n");
				}
				return;

			case "error":
				try {
					lh.addEntry(data,offset,length);
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					e.printStackTrace();
					assertTrue(true);
					return;
				} catch (InterruptedException | BKException e) {
					e.printStackTrace();
				}
				break;

			case "null":
				try {
					lh.addEntry(data,offset,length);
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
					assertTrue(true);
					return;
				} catch (InterruptedException | BKException | ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				break;

			default:
				Assert.fail("Fail addEntryTest:\nExpected: "+expected
						+"\ndata: "+Arrays.toString(data)
						+"\noffset: "+offset
						+"\nlength: "+length
						+"\n");
		}



	}
}
