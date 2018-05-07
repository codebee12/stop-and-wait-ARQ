
/**
 *
 * @author Ekta & Sanjana
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

class GClient
{
    
    //private static final int PORT = 6789;
    public static byte[] MESSAGE_START = { 0x52, 0x44, 0x54 }; // "RDT "
    public static byte[] MESSAGE_END = { 0x45, 0x4e, 0x44, 0xa, 0xd }; //" END CRLF"
    public static byte[] MESSAGE_NORMAL = { 0xa, 0xd }; //"CRLF"
    public static int MESSAGE_FRONT_OFFSET = 4; //"RDT#"
    public static int MESSAGE_BACK_OFFSET = 2; //"CRLF"
    public static int MESSAGE_LAST_BACK_OFFSET = 5; //"ENDCRLF"
    
    
    public static void main(String[] args) throws IOException 
    {
        String MYFILENAME = "my";
        int len=args.length;
        int i;
        String address=args[0];//EXTRACTING SERVER IP ADDRESS
        System.out.println("IP Address: "+address);
        int PORT= Integer.parseInt(args[1]);//EXTRACTING PORT 
        System.out.println("Port: "+PORT);
        String fileName=args[2];// EXTRACTING FILE NAME
        System.out.println("Filename "+fileName);
        MYFILENAME=MYFILENAME+fileName;
        int ackToForget[]=new int[len];
        
        int flag[]=new int[len];
        for(i=3;i<len;i++) // Creating an array of consingments to Forget
        {
            ackToForget[i]=Integer.parseInt(args[i]);
            flag[i]=0;
        }
        Arrays.sort(ackToForget);
        //ackToForget.sort();
        i=0;
        // Create a server socket
	DatagramSocket clientSocket = new DatagramSocket(); 
        InetAddress ip=InetAddress.getByName(address);//CONVERTING TO INETADDRESS OBJECT
        
        
        System.out.println("Requesting "+fileName+" from "+address+" port: "+PORT);
        fileName="REQUEST"+fileName+"CRLF";
        byte[] file= fileName.getBytes();
        DatagramPacket requestFile=new DatagramPacket(file,file.length,ip,PORT);//REQUESTING FILE
        clientSocket.send(requestFile);
        int BUFFER_SIZE=521;
	// Set up byte arrays for sending/receiving data
        byte[] receiveData = new byte[ BUFFER_SIZE ];
        byte[] dataForSend = new byte[ BUFFER_SIZE ];
        
        int p=1,flag1=0;
        try 
        {
        File myFile;
        FileOutputStream myFOS;
        myFile = new File(MYFILENAME);
        myFOS = new FileOutputStream(myFile);
        byte[] data = new byte[521]; // each consignment has data length 521 bytes
        int count;                  // for copying / extracting from msg to data
        int ck=0;
        byte seqNum=0;
        DatagramPacket received ;
         // Get the received packet
            received = new DatagramPacket( receiveData, receiveData.length );
            boolean z=true;
            int port=0;
            InetAddress IPAddress=received.getAddress();
            while(z==true)
            {
                boolean w=true,x=true;
                clientSocket.receive( received );
            
                // Get the message from the packet
                int message = ByteBuffer.wrap(received.getData( )).getInt();
                // Get packet's IP and port
                IPAddress = received.getAddress();
                port = received.getPort();// THIS PORT IS DIFFERENT FROM "PORT"
                p=message+1;
                 byte a[]=new byte[received.getLength()];
                a=received.getData();
                seqNum=a[3];
                ck=(seqNum+1)%128;
                if(ck>127)
                    ck=0;
                // String st="ACK"+((seqNum+1)%128)+"CRLF";
                String st="ACK"+(ck)+"CRLF";
                dataForSend = st.getBytes();
                int l;
                l=RindexOf(received.getData());
                if(l>-1)
                {
                    x=false;
                    System.out.println("Last Consignment");
                }    
                for(i=3;i<len;i++)
                {
                if(ck==ackToForget[i]&& flag[i]==0)
                {
                    flag[i]=1;
                    if(i==len-1)
                        z=true;
                    break;
                }
                else if(ck==ackToForget[i] && flag1==1)
                {
                    flag[i]=2;
                    break;
                }
                }
                if(i==len)
                    i=i-1;
                //System.out.println(i+ " " +flag[i] + " " +p);
                if(flag[i]==1)
                {
                    flag1=1;
                    System.out.println("Received CONSIGNMENT: " + (seqNum));
                    //System.out.println("Forgot ACK "+((seqNum+1))%128);
                    System.out.println("Forgot ACK "+ck);
                    flag[i]=flag[i]+1;
                }
                else if(flag[i]==2)
                {
                    System.out.println("Received CONSIGNMENT: " + (seqNum) + " duplicate - discarding");
                    flag[i]=flag[i]+1;
                    w=false;
                    DatagramPacket packet = new DatagramPacket( dataForSend, dataForSend.length, IPAddress, port );
                    clientSocket.send( packet ); 
                //System.out.println("Sent ACK "+((seqNum+1))%128);
                
                flag1=0;
                if(z==false)
                    break;
                else
                {
                    if(x==true)
                        System.out.println("Sent ACK "+ck);
                }
            }
            else
            {
                
                System.out.println("Received CONSIGNMENT: " + (seqNum));
                // Send the ACK to the server
                DatagramPacket packet = new DatagramPacket( dataForSend, dataForSend.length, IPAddress, port );
                clientSocket.send( packet ); 
                //System.out.println("Sent ACK "+((seqNum+1))%128);
                if(z==false)
                    break;
                else
                {
                    if(x==true)
                        System.out.println("Sent ACK "+ck);
                }
                    
            }
             
                if(l>-1)
                {
                    if(w==true)
                        myFOS.write(received.getData(),MESSAGE_FRONT_OFFSET,l-MESSAGE_FRONT_OFFSET);
                    
                    z=false;
                }
                else //if (!matchByteSequence(received.getData(), received.getData().length-MESSAGE_END.length , MESSAGE_END.length, MESSAGE_END)) 
                {
                    if(w==true)
                        myFOS.write(received.getData(), MESSAGE_FRONT_OFFSET, received.getData().length-MESSAGE_FRONT_OFFSET-MESSAGE_BACK_OFFSET-3);
                    
                    for (count=0; count < received.getData().length-MESSAGE_FRONT_OFFSET-MESSAGE_BACK_OFFSET; count++) {
                        data[count] = received.getData()[MESSAGE_FRONT_OFFSET+count];
                    }   
                } 
            
            
            } 
            
            myFOS.close();
        }
        catch (FileNotFoundException ex) 
            {
                System.out.println(ex.getMessage());
            } 
            catch (IOException ex) 
            {
                System.out.println(ex.getMessage());
            }
              //System.out.println( "Oops, packet with sequence number "+ message + " was dropped");
        clientSocket.close();
    }
    
   static public int RindexOf(byte[] outerArray) 
   {
       byte[] smallerArray={ 0x45, 0x4e, 0x44, 0xa, 0xd }; //" END CRLF"
       for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) 
       {
        boolean found = true;
        for(int j = 0; j < smallerArray.length; ++j) 
        {
           if (outerArray[i+j] != smallerArray[j])
           {
               found = false;
               break;
           }
        }
        if (found) 
            return i;
        }
        return -1;  
   }
    static public boolean matchByteSequence(byte[] input, int offset, int length, byte[] ref) {
        
        boolean result = true;
        if (length == ref.length) {
            for (int i=0; i<ref.length; i++) {
                if (input[offset+i] != ref[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
}
