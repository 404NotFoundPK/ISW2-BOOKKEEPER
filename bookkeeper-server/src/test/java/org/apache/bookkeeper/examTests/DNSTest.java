package org.apache.bookkeeper.examTests;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;

import javax.naming.NamingException;

import org.apache.bookkeeper.net.DNS;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;

@RunWith(Enclosed.class)
public class DNSTest{

	@BeforeClass
	public static void mockSetUp()
	{
		System.out.println("TRYING-START:");
		MockDNS.getInstance(53).start();
	}
	@AfterClass
	public static void mockSetDown()
	{
		MockDNS.getInstance(53).stop();
	}

	@RunWith (value=Parameterized.class)
	public static class DNS1 {

		private InetAddress address;
		private String nameserver;
		private String expected;

		private void configure(String expected, String ip, String nameserver)
		{
			try {
				if(ip!=null)
					address = InetAddress.getByName(ip);
				else
					address = null;
				this.nameserver = nameserver;
				this.expected = expected;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}

		public DNS1(String expected, String ip, String nameserver)
		{
			configure(expected, ip, nameserver);
		}

		@Parameters
		public static Collection<String[]> getTestParameters()
		{
			return Arrays.asList(new String[][] {
					//Fornire il un ip(primo campo) non valido non è
					//da considerare poiché è un valore che non arriverà
					//mai al nostro metodo. Verrebbe gestito prima.

					/**
					 *Category Partition Analysis:
					 *InetAddress hostIp:
					 *	DummyCatergories:{null},{not_null}
					 *	NotTrivialCategories:{special_ips_v4},{private_ips_v4},{not_private_ips_v4},{special_ips_v6},{private_ips_v6},{valid_ips_v6}
					 *
					 *String ns:
					 *DummyCatergories:{null},{not_null}
					 *NotTrivialCategories:{valid_local_namesarver},{valid_notlocal_nameserver},{notvalid_nameserver}
					 *
					*/

					//Test con approccio Monodimensionale
					//Trivial
					{"null",		null,				null},								//{null},			{null}
					{"null",		null,				"localhost"},						//{null},			{not_null}
					//{"null","127.0.0.1",null},											//{not_null},		{null} -> BUG


					{"error",		"0.0.0.0",			"8.8.8.8"},							//{special_ips_v4},	{valid_notlocal_namesarver}
					{"error",		"255.255.255.255",	"8.8.8.8"},							//{special_ips_v4},	{valid_notlocal_namesarver} - BIS
					{"error",		"192.168.0.232",	"8.8.8.8"},							//{private_ips_v4},	{valid_notlocal_namesarver}
					{"error",		"10.0.23.1",		"8.8.8.8"},							//{private_ips_v4},	{valid_notlocal_namesarver} - BIS
					{"google",		"8.8.8.8",			"8.8.8.8"},							//{valid_ips_v4},	{valid_notlocal_namesarver}

					{"error",		"0.0.0.0",			"localhost"},						//{special_ips_v4},	{valid_local_namesarver}
					{"error",		"255.255.255.255",	"localhost"},						//{special_ips_v4},	{valid_local_namesarver} - BIS
					{"error",		"192.168.0.142",	"localhost"},						//{private_ips_v4},	{valid_local_namesarver}
					{"error",		"10.0.23.1",		"localhost"},						//{private_ips_v4},	{valid_local_namesarver} - BIS
					//{"mock.name",	"8.8.8.8",			"localhost"},						mutation//{valid_ips_v4},	{valid_local_namesarver}

					/*NOTA: osservare che "::" non fa scattare l'eccezione
                    NamingException("IPV6") a riga 75, quando invece dovrebbe.*/
					{"error",		"::",				"localhost"},						//{special_ips_v6},	{valid_local_namesarver}
					{"error",		"0000:0000:0000:0000:0000:0000:0000:0000","localhost"},	//{special_ips_v6},	{valid_local_namesarver} - BIS

					{"error",		"::ffff:c0a8:1",	"localhost"},						//{private_ips_v6},	{valid_local_namesarver}
					{"error",		"::ffff:d83a:c603",	"localhost"},						//{valid_ips_v6},	{valid_local_namesarver}

					{"error",		"8.8.8.8",			"255.255.255.255"}					//{valid_ips_v4},	{notvalid_nameserver}





					/*
                    //valid name server
                    {"error","0:0:0:0:0:FFFF:7F00:0001","8.8.8.8"},	//ipv6 valido senza riscontro dns
                    {"error","1.2.3.4","localhost"},				//ipv4 valido senza riscontro dns
                    {"google","8.8.8.8","8.8.8.8"},					//ipv4 valido con riscontro dns
                    {"error","2002:0808:0808::0808:0808","8.8.8.8"},//ipv6 error

                    //invalid name server
                    {"error","127.0.0.1","-8.8.8.8"},				//ipv4 valido senza riscontro dns
                    {"error","8.8.8.8","-8.8.8.8"},					//ipv4 valido con riscontro dns



                    */
					//null
					//{"error", "127.0.0.1", null}
			});
		}

		@Test
		public void reverseDnsTest()
		{
			Exception exc = null;

			switch(expected)
			{
				case "error":
					try {
						DNS.reverseDns(address, nameserver);
					}
					catch (NamingException e) {
						assertTrue(true);
						return;
					}
					Assert.fail("Fail:\nExpected: "+expected+"\nAddress: "+address+"\nNameserver: "+nameserver+"\n");
					break;

				case "null":
					try {
						DNS.reverseDns(address, nameserver);
					}	catch (NullPointerException | NamingException e) {
						exc=e;
						if(!(e instanceof NamingException)) {
							assertTrue(true);
							return;
						}
					}
					Assert.fail("Fail: Expected: "+expected+" Address: "+address+" Nameserver: "+nameserver+ " Exception: " + exc);
					break;
				default:
					try {
						System.out.println("Ciao!! :: " + DNS.reverseDns(address, nameserver));
						assertEquals(true,DNS.reverseDns(address, nameserver).contains(expected));
						return;
					}catch (NamingException e) {
						e.printStackTrace();
						Assert.fail("Fail: Expected: "+expected+" Address: "+address+" Nameserver: "+nameserver);
					}
			}
		}


	}

	@RunWith (value=Parameterized.class)
	public static class DNS2 {

		private String strInterface;
		private String expected;
		private boolean returnSubinterfaces;


		private void configure(String expected, String strInterface, String returnSubinterfaces)
		{
			if(strInterface != null && (strInterface.equals("available") || strInterface.equals("not_available")))
			{
				try {
					boolean isFounded = false;
					Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
					NetworkInterface networkInterface;
					while(interfaces.hasMoreElements() && isFounded == false)
					{
						networkInterface=interfaces.nextElement();
						if(strInterface.equals("available"))
						{
							if(networkInterface.isUp() && networkInterface.getName() != "lo")
							{
								this.strInterface = networkInterface.getName();
								this.expected = expected;
								this.returnSubinterfaces = Boolean.parseBoolean(returnSubinterfaces);
								isFounded = true;
								System.out.println(this.strInterface);
							}
						}
						else if(strInterface.equals("not_available"))
						{
							if(!networkInterface.isUp() && networkInterface.getName() != "lo")
							{
								this.strInterface = networkInterface.getName();
								this.expected = expected;
								this.returnSubinterfaces = Boolean.parseBoolean(returnSubinterfaces);
								isFounded = true;
								System.out.println(this.strInterface);
							}
						}

					}
					if(!isFounded)
						Assert.fail("No interfaces in the system for testing");
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
			else
			{
				this.strInterface = strInterface;
				this.expected = expected;
				this.returnSubinterfaces = Boolean.parseBoolean(returnSubinterfaces);
			}
		}

		public DNS2(String expected, String strInterface, String returnSubinterfaces)
		{
			configure(expected, strInterface,returnSubinterfaces);
		}

		@Parameters
		public static Collection<String[]> getTestParameters()
		{
			return Arrays.asList(new String[][] {

					/**
					 *Category Partition Analysis:
					 *String strInterface:
					 *	DummyCatergories:{null},{not_null}
					 *	NotTrivialCategories:{undefined},{not_available},{available},{special_string}
					 *
					 *Boolean returnSubinterrfaces:
					 *	Values:{true,false},{null}
					 *
					*/

					//Approccio multidimensionale:

					//accept Ips From subinterfaces
					{"down",	"not_available",	"true"},			//{not_available},	{true}
					//{"ok",		"available",		"true"},			//{available},		{true}  -> Mutation bug
					{"ok",		"default",			"true"},			//{special_string},	{true}
					{"error", 	"-1", 				"true"},			//{undefined},		{true}
					{"error", 	null, 				"true"},			//{null},			{true}

					//not accept Ips From subinterfaces
					{"down",	"not_available",	"false"},			//{not_available},	{false}
					//{"ok",		"available",		"false"},			//{available},		{false} -> Mutation bug
					{"ok",		"default",			"false"},			//{special_string},	{false}
					{"error", 	"-1", 				"false"},			//{undefined},		{false}
					{"error", 	null, 				"false"},			//{null},			{false}

					{"error", 	null,				 null}
			});
		}


		@Test
		public void getDefaultIPTest()
		{
			String ip;
			//System.out.print("\n\n\n"+"expected: "+expected+"\n");
			//System.out.print("strInterface: "+strInterface+"\n\n");

			switch(expected)
			{
				case "ok":
					try {
						ip=DNS.getDefaultIP(strInterface);
						assertEquals(true,isValidInet4Address(ip));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						Assert.fail("Fail getDefaultIPTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\n");
					}
					break;
				case "error":
					if(strInterface == null)
					{
						try {
							DNS.getDefaultIP(strInterface);
						} catch (NullPointerException e) {
							assertTrue(true);
							return;
						}
						catch (UnknownHostException e) {
							Assert.fail("Fail getDefaultIPTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\n");
						}
					}
					else
					{
						try {
							DNS.getDefaultIP(strInterface);
						} catch (UnknownHostException e) {
							assertTrue(true);
							return;
						}
					}
					break;
				case "down":
					try {
						ip=DNS.getDefaultIP(strInterface);
						//Vedere caso test per getIps().
						assertEquals(true, isValidInet4Address(ip));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						Assert.fail("Fail getDefaultIPTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\n");
					}
					break;



				default:
					Assert.fail("Fail getDefaultIPTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\n");
			}
		}

		@Test
		public void getIPsTest()
		{
			String[] iPs,iPsSub;
			boolean check=true;


			switch(expected)
			{
				case "ok":
					try {
						iPs=DNS.getIPs(strInterface, returnSubinterfaces);
						if(iPs.length>0)
						{
							for(int i=0; i<iPs.length; i++)
							{
								if(!isValidInet4Address(iPs[i]))
								{
									check=false;
									break;
								}
							}
						}
						else
							check=false;

						assertEquals(true,check);
					}
					catch (UnknownHostException e) {
						e.printStackTrace();
						Assert.fail("Fail getIPsTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nreturnSubinterfaces: "+returnSubinterfaces+"\n");
					}
					if(returnSubinterfaces==false)
					{
						try {
							iPs=DNS.getIPs(strInterface, returnSubinterfaces);
							iPsSub=DNS.getIPs(strInterface, !returnSubinterfaces);
							assertEquals(true,iPsSub.length>=iPs.length);
						}
						catch (UnknownHostException e) {
							e.printStackTrace();
							Assert.fail("Fail getIPsTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nreturnSubinterfaces: "+returnSubinterfaces+"\n");
						}

					}
					break;

				case "error":
					if(strInterface == null)
					{
						try {
							DNS.getIPs(strInterface, returnSubinterfaces);
						} catch (NullPointerException e) {
							assertTrue(true);
							return;
						}
						catch (UnknownHostException e) {
							Assert.fail("Fail getIPsTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nreturnSubinterfaces: "+returnSubinterfaces+"\n");
						}
					}
					else
					{
						try {
							DNS.getIPs(strInterface, returnSubinterfaces);
						} catch (UnknownHostException e) {
							assertTrue(true);
							return;
						}
					}
					break;

				case "down":
					try {
						iPs=DNS.getIPs(strInterface, returnSubinterfaces);
						//Si attende l'indirizzo IP della sola interfaccia e non altri, poiché l'interfaccia risulta "down".
						assertEquals(true, iPs.length==1 && isValidInet4Address(iPs[0]));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						Assert.fail("Fail getDefaultIPTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\n");
					}

					break;

				default:
					Assert.fail("Fail getIPsTest:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nreturnSubinterfaces: "+returnSubinterfaces+"\n");
			}
		}

		private static boolean isValidInet4Address(String ip)
		{
			try {
				if ( ip == null || ip.isEmpty() ) {
					return false;
				}

				String[] parts = ip.split( "\\." );
				if ( parts.length != 4 ) {
					return false;
				}

				for ( String s : parts ) {
					int i = Integer.parseInt( s );
					if ( (i < 0) || (i > 255) ) {
						return false;
					}
				}
				if ( ip.endsWith(".") ) {
					return false;
				}

				return true;
			} catch (NumberFormatException nfe) {
				return false;
			}
		}


	}

	@RunWith (value=Parameterized.class)
	public static class DNS3 {

		private String strInterface;
		private String expected;
		private String nameserver;


		private void configure(String expected, String strInterface, String nameserver)
		{
			if(strInterface != null && (strInterface.equals("available") || strInterface.equals("not_available")))
			{
				try {
					boolean isFounded = false;
					Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
					NetworkInterface networkInterface;
					while(interfaces.hasMoreElements() && isFounded == false)
					{
						networkInterface=interfaces.nextElement();
						if(strInterface.equals("available"))
						{
							if(networkInterface.isUp() && networkInterface.getName() != "lo")
							{
								this.strInterface = networkInterface.getName();
								this.expected = expected;
								this.nameserver = nameserver;
								isFounded = true;
								System.out.println(this.strInterface);
							}
						}
						else if(strInterface.equals("not_available"))
						{
							if(!networkInterface.isUp() && networkInterface.getName() != "lo")
							{
								this.strInterface = networkInterface.getName();
								this.expected = expected;
								this.nameserver = nameserver;
								isFounded = true;
								System.out.println(this.strInterface);
							}
						}

					}
					if(!isFounded)
						Assert.fail("No interfaces in the system for testing");
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
			else
			{
				this.strInterface = strInterface;
				this.expected = expected;
				this.nameserver = nameserver;
			}
		}

		public DNS3(String expected, String strInterface, String nameserver)
		{
			configure(expected, strInterface,nameserver);
		}

		@Parameters
		public static Collection<String[]> getTestParameters()
		{
			return Arrays.asList(new String[][] {

					/**
					 *Category Partition Analysis:
					 *String strInterface:
					 *	DummyCatergories:{null},{not_null}
					 *	NotTrivialCategories:{undefined},{not_available},{available},{special_string}
					 *
					 *
					 *String nameserver:
					 *DummyCatergories:{null},{not_null}
					 *NotTrivialCategories:{valid_local_namesarver},{valid_notlocal_nameserver},{notvalid_nameserver}
					 *
					*/

					{"error",	"not_available",	"255.255.255.255"},			//{not_available},	{notvalid_nameserver}
					{"error",	"available",		"255.255.255.255"},			//{available},		{notvalid_nameserver}
					{"error",	"default",			"255.255.255.255"},			//{special_string},	{notvalid_nameserver}
					{"error", 	"-1", 				"255.255.255.255"},			//{undefined},		{notvalid_nameserver}
					{"error", 	null, 				"255.255.255.255"},			//{null},			{notvalid_nameserver}


					{"down",	"not_available",	"8.8.8.8"},					//{not_available},	{valid_notlocal_nameserver}
					{"ok",		"available",		"8.8.8.8"},					//{available},		{valid_notlocal_nameserver}
					{"ok",		"default",			"8.8.8.8"},					//{special_string},	{valid_notlocal_nameserver}
					{"error", 	"-1", 				"8.8.8.8"},					//{undefined},		{valid_notlocal_nameserver}
					{"error", 	null, 				"8.8.8.8"},					//{null},			{valid_notlocal_nameserver}


					{"down",	"not_available",	"local"},					//{not_available},	{valid_local_namesarver}
					{"ok",		"available",		"local"},					//{available},		{valid_local_namesarver}
					{"ok",		"default",			"local"},					//{special_string},	{valid_local_namesarver}
					{"error", 	"-1", 				"local"},					//{undefined},		{valid_local_namesarver}
					{"error", 	null, 				"local"},					//{null},			{valid_local_namesarver}

					{"error", 	null, 				null},						//{null},			{null}

					{"down",	"not_available",	null},						//{not_available},	{null}
					{"ok",		"available",		null},						//{available},		{null}
					{"ok",		"default",			null},						//{special_string},	{null}
					{"error",	"-1", 				null},						//{undefined},		{null}
					{"error", 	null, 				null},						//{null},			{null}


			});
		}
		@Test
		public void getHosts()
		{
			String[] hosts;

			switch(expected)
			{
				case "ok":
					try {
						hosts = DNS.getHosts(strInterface, nameserver);
						System.out.println("HOSTS: "+strInterface+nameserver+" -> "+Arrays.toString(DNS.getHosts(strInterface, nameserver)));
						assertEquals(true, hosts.length>0);
					} catch (UnknownHostException | NullPointerException e) {
						e.printStackTrace();
						Assert.fail("Fail getHosts:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nnameserver"+nameserver+"\n");
					}
					break;

				case "error":
					if(strInterface == null)
					{
						try {
							DNS.getHosts(strInterface, nameserver);
						} catch (NullPointerException e) {
							assertTrue(true);
							return;
						}
						catch (UnknownHostException e) {
							Assert.fail("Fail getHosts:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nnameserver"+nameserver+"\n");

						}
					}
					else
					{
						try {
							DNS.getHosts(strInterface, nameserver);
						} catch (UnknownHostException e) {
							assertTrue(true);
							return;
						}
					}
					break;

				case "down":
					try {
						hosts = DNS.getHosts(strInterface, nameserver);
						assertEquals(true, hosts.length==1);
					} catch (UnknownHostException | NullPointerException e) {
						e.printStackTrace();
						Assert.fail("Fail getHosts:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nnameserver"+nameserver+"\n");
					}
					break;

				default:
					Assert.fail("Fail getHosts:\nExpected: "+expected+"\nstrInterface: "+strInterface+"\nnameserver"+nameserver+"\n");

			}

			if(expected.equals("ok"))
			{

			}
			else
			{

			}
		}
	}
}


