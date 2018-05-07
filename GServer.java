/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Ekta & Sanjana
 */
import java.io.*;
import java.net.*;
import java.nio.*;
class GServer
{
    private static final int BUFFER_SIZE = 512;
    public static int CONSIGNMENT = 512;
    public static  byte[] RDT = new byte[] { 0x52, 0x44, 0x54 };
    public static byte[] END = new byte[] { 0x45, 0x4e, 0x44 };
    public static byte[] CRLF = new byte[] { 0x0a, 0x0d };

    public static void main(String args[]) throws Exception
    {
        byte[] SEQ_0 = new byte[] { 0x30 };
        int len,i;
        len = args.length;
        int cns_to_forget[]=new int[len];
        int flag[]= new int[len];
        int PORT=Integer.parseInt(args[0]);// Getting port from command Line
        for(i=1;i<len;i++) // Creating an array of consingments to Forget
        {
        cns_to_forget[i]=Integer.parseInt(args[i]);
        flag[i]=0;
        }
        DatagramSocket serverSocket = new DatagramSocket(PORT); 
        byte sequenceNumber =0;
        byte[] rdfile = new byte[521];
        DatagramPacket rp=new DatagramPacket(rdfile,rdfile.length);
        serverSocket.receive(rp);
        String fileName=new String(rp.getData());
        fileName=fileName.trim();
        InetAddress clientIP = rp.getAddress(); 
        int clientPort = rp.getPort();
        fileName=fileName.replace("CRLF","");
        fileName=fileName.trim();
        fileName=fileName.substring(7,(fileName.length()));
        System.out.println("Received request for "+fileName+" from "+rp.getAddress() +" "+PORT);//DISPLAYING REQUIRED MSG..
        SEQ_0[0]=sequenceNumber;
        FileInputStream myFIS = null;
        byte[] myData = new byte[512];
        byte[] myLastData;
        byte[] myMsg = new byte[521];
        int bytesRead = 0;
        int j; // counter for copying bytes in array
        boolean FileSent=false;
        int d=-1;
        try 
        {
            myFIS = new FileInputStream(fileName);
            bytesRead = myFIS.read(myData);
            while(!FileSent)
            { 
                if(bytesRead != -1) 
                {
                    d++;
                    //System.out.println("test a"+d);
                    SEQ_0[0]=sequenceNumber;
                    if (bytesRead > -1) 
                    {
                        if (bytesRead < CONSIGNMENT) 
                        {
                            // last consignment
                            // make a special byte array that exactly fits the number of bytes read 
                            // otherwise, the consignment may be padded with junk data
                            myLastData = new byte[bytesRead];
                            for (j=0; j<bytesRead; j++) 
                            {
                                myLastData[j] = myData[j];
                            }
                            myMsg = concatenateByteArrays(RDT, SEQ_0, myLastData, END, CRLF);
                            FileSent=true;
                        } 
                        else 
                        {
                            myMsg = concatenateByteArrays(RDT, SEQ_0, myData, CRLF);
                        }
                        //byte[] receiveData = new byte[ BUFFER_SIZE ];
                        for(i=1;i<len;i++)
                        {
                            if(cns_to_forget[i]==sequenceNumber && flag[i]==0)
                            {
                                FileSent=false;
                                flag[i]=1;
                                break;
                            }
                        }
            
                        if(i==len)//last one
                            i--;
                        if(flag[i]==1)
                        {
                            System.out.println("Forgot CONSIGNMENT "+cns_to_forget[i]);
                            flag[i]=flag[i]+1;
                        }
                        else 
                        {
                            //System.out.println("Hello");
                            sendFrame(myMsg,sequenceNumber,serverSocket,clientIP,clientPort);
                            if(sequenceNumber==127)
                                sequenceNumber=0;
                            else
                                sequenceNumber++;
                        try
                        {
                            //System.out.println(flag[i]);
                            serverSocket.setSoTimeout(30);
                            if(FileSent==true)
                                break;
                            else
                            receiveFrame(serverSocket);
                        } 
                        catch( SocketTimeoutException exception )
                        {
                        // If we don't get an ack, prepare to resend sequence number
                            if(sequenceNumber!=0)
                                sequenceNumber--;
                            System.out.println( "Timeout (Sequence Number " + sequenceNumber + ")" );
                            if(flag[i]==2)
                            {
                                //System.out.println("Hola");
                                sendFrame(myMsg,sequenceNumber,serverSocket,clientIP,clientPort);
                                receiveFrame(serverSocket);
                                if(sequenceNumber==127)
                                    sequenceNumber=0;
                                else
                                    sequenceNumber++;
                            }
                            else
                            {
                                sendFrame(myMsg,sequenceNumber,serverSocket,clientIP,clientPort);
                                //System.out.println("much");
                                receiveFrame(serverSocket);
                                if(sequenceNumber==127)
                                    sequenceNumber=0;
                                else
                                    sequenceNumber++;
                                
                            }
                            }
                         bytesRead = myFIS.read(myData);
                        }
                    }    
                }
            }
        }
        catch (FileNotFoundException ex1) 
        {
            System.out.println(ex1.getMessage());
        } 
        catch (IOException ex) 
        {
            System.out.println(ex.getMessage());
        }
        finally 
        {
            try 
            {
                myFIS.close();
                
            } 
            catch (IOException ex) 
            {
		System.out.println(ex.getMessage());
            }	
        }
        System.out.println("END");
        serverSocket.close();
        }
    
    public static void sendFrame( byte myMsg[], int sequenceNumber, DatagramSocket serverSocket,InetAddress clientIP,int clientPort) throws IOException
    {
        System.out.println( "Sending CONSIGNMENT " + sequenceNumber );				
                // Get byte data for message 
                //myMsg = ByteBuffer.allocate(4).putInt( sequenceNumber ).array();
                sequenceNumber++;
                    // Send the UDP Packet to the server
                    DatagramPacket packet = new DatagramPacket(myMsg, myMsg.length, clientIP, clientPort);
                    serverSocket.send( packet );
                    // Receive the server's packet
                  
    }
    public static void receiveFrame(DatagramSocket serverSocket) throws IOException
    {
        byte[] receiveData = new byte[ BUFFER_SIZE ];
        // Receive the server's packet
                    //serverSocket.setSoTimeout(1000);
                    DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive( received );
                    String dt=new String(received.getData());
                    dt=dt.replace("ACK", "");
                    dt=dt.replace("CRLF","");
                    dt=dt.trim();
                    System.out.println("Received ACK "+dt);
    }
    public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d) 
    {
        byte[] result = new byte[a.length + b.length + c.length + d.length]; 
        System.arraycopy(a, 0, result, 0, a.length); 
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length+b.length, c.length);
        System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        return result;
    }
    
    public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e) {
        byte[] result = new byte[a.length + b.length + c.length + d.length + e.length]; 
        System.arraycopy(a, 0, result, 0, a.length); 
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length+b.length, c.length);
        System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        System.arraycopy(e, 0, result, a.length+b.length+c.length+d.length, e.length);
        return result;
    }
     public static String byteToHex(byte b) {
        int i = b & 0xFF;
        return Integer.toHexString(i);
    }

}
