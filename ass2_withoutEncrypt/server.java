import java.io.*;
import java.net.*;
import java.util.*;
// import org.javatuples.Triplet;

public class server
{
  public static Hashtable<String,Triplet> userSendTable = new Hashtable<String,Triplet>();
  public static Hashtable<String,Triplet> userReceiveTable  = new Hashtable<String,Triplet>();
}

class Triplet
{
  public Socket a;
  public BufferedReader b;
  public DataOutputStream c;
  Triplet(Socket a, BufferedReader b, DataOutputStream c)
  {
    this.a=a;
    this.b=b;
    this.c=c;
  }
  Socket getValue0()
  {
    return a;
  }
  BufferedReader getValue1()
  {
    return b;
  }
  DataOutputStream getValue2()
  {
    return c;
  }
}

class TCPServer {


  public static void main(String args[]) throws IOException
    {
      // userSendTable = new Hashtable<String,Socket>();
      // userSendTable = new Hashtable<String,Socket>();

      ServerSocket welcomeSocket = new ServerSocket(6789);

      while(true) {

        Socket connectionSocket = welcomeSocket.accept();

        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

	      SocketThread socketThread = new SocketThread(connectionSocket, inFromClient, outToClient);

        Thread thread = new Thread(socketThread);

        thread.start();

      }

    }
}


class SocketThread implements Runnable {
     String clientSentence;
     String capitalizedSentence;
     Socket connectionSocket;
     BufferedReader inFromClient;
     DataOutputStream outToClient;

     SocketThread (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient) {
	      this.connectionSocket = connectionSocket;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
     }

     boolean checkUsername(String name)
     {
       int flag=1;
       for(int i=0;i<name.length();i++)
       {
         int c = (int)name.charAt(i);
         if( (c>=65 && c<=91) || (c>=97 && c<=122) || (c>=48 && c<=57) )
            continue;
        // System.out.println("PRINTING FALSE "+ c);
         return false;
       }
       return true;
     }


    public void run()
    {
       try
       {
         clientSentence = inFromClient.readLine();
         // System.out.println(clientSentence);
         if(!(clientSentence.substring(0,15).equals("REGISTER TORECV") ||  clientSentence.substring(0,15).equals("REGISTER TOSEND")))
         {
           outToClient.writeBytes("ERROR 101 No user registered\n\n");
           return;
         }
         if(clientSentence.split(" ").length!=3)
         {
           outToClient.writeBytes("ERROR 101 No user registered\n\n");
           return;
         }
         String dummy=inFromClient.readLine();
         String[] split_input = clientSentence.split(" ",3);
         Triplet userSocket = new Triplet (connectionSocket, inFromClient, outToClient);
         String username = split_input[2];
         if(!checkUsername(username))
         {
           outToClient.writeBytes("ERROR 100 MALFORMED USERNAME\n\n");
         }

         if(clientSentence.substring(0,15).equals("REGISTER TORECV"))
         {
           server.userReceiveTable.put(username,userSocket);
           outToClient.writeBytes("REGISTERED TORECV "+username+"\n\n");
           System.out.println("REGISTERED to recv: "+ username);

           return;
         }

         server.userSendTable.put(username, userSocket);
         outToClient.writeBytes("REGISTERED TOSEND "+username+"\n\n");
         System.out.println("REGISTERED to send: "+ username);


         while(true)
         {
           String recipient = inFromClient.readLine();

           if(recipient.split(" ").length!=2 || !(recipient.split(" ",2)[0].equals("SEND")))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete1\n\n");
              continue;
           }



           String content_length = inFromClient.readLine();
           if(content_length.split(" ").length!=2 || !(content_length.split(" ",2)[0].equals("Content-length:")))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete2\n\n");
              continue;
           }

           // System.out.println(content_length+"  content_length");

           dummy = inFromClient.readLine();
           if(!dummy.equals(""))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete3\n\n");
              continue;
           }
           // dummy = inFromClient.readLine();

           // System.out.println(dummy+"  dummy");
           String recipentUsername = recipient.split(" ",2)[1];
           String message="";
           int count = Integer.parseInt(content_length.split(": ",2)[1]);
           int value=0;
           while(count>0)
           {
               message+=(char)inFromClient.read();
               count--;
               // System.out.println(message);
           }

           // String message = inFromClient.readLine();



           // System.out.println(username+ " " + content_length+" "+message+ " "+ recipentUsername);

           if (! server.userReceiveTable.containsKey(recipentUsername))
           {
              outToClient.writeBytes("ERROR 102 Unable to send\n\n");
           }
           else
           {
               Triplet recipientSocket = server.userReceiveTable.get(recipentUsername);

               DataOutputStream outToClient_recipient =recipientSocket.getValue2();
               BufferedReader inFromClient_recipient = recipientSocket.getValue1();

               outToClient_recipient.writeBytes("FORWARD "+username+"\n"+ content_length +"\n\n"+ message);

               String ack_reipient = inFromClient_recipient.readLine();

               // System.out.println(ack_reipient);
               // System.out.println("RECEIVED "+username);

               if(ack_reipient.equals("RECEIVED "+username))
               {
                 // System.out.println("yaha aa rh hai");
                    outToClient.writeBytes("SENT " + recipentUsername +"\n\n");
                    String dummy3 = inFromClient_recipient.readLine(); // \n from the recipient after received ack
               }
               else
                   outToClient.writeBytes("ERROR 102 Unable to send\n\n");
            }






           // System.out.println("REGISTERED SEND : "+ username);

  	     //       clientSentence = inFromClient.readLine();
         //
  		   // System.out.println(clientSentence);
         //
    	   //       capitalizedSentence = clientSentence.toUpperCase() + '\n';
         //
         //  	   outToClient.writeBytes(capitalizedSentence);
        }
      }
	    catch(Exception e)  {
		        try
            {
			           connectionSocket.close();
		        } catch(Exception ee) { }
		        // break
	      }
    }
}