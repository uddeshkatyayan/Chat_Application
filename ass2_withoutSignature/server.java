import java.io.*;
import java.net.*;
import java.util.*;

public class server
{
  public static Hashtable<String,Quadruple> userSendTable = new Hashtable<String,Quadruple>();
  public static Hashtable<String,Quadruple> userReceiveTable  = new Hashtable<String,Quadruple>();
}

class Quadruple
{
  public Socket a;
  public BufferedReader b;
  public DataOutputStream c;
  public String d;
  Quadruple(Socket a, BufferedReader b, DataOutputStream c,String d)
  {
    this.a=a;
    this.b=b;
    this.c=c;
    this.d=d;
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
  String getValue3()
  {
    return d;
  }
}

class TCPServer {


  public static void main(String args[]) throws IOException
    {

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
         return false;
       }
       return true;
     }


    public void run()
    {
       try
       {
         clientSentence = inFromClient.readLine();
        
         if(!(clientSentence.substring(0,15).equals("REGISTER TORECV") ||  clientSentence.substring(0,15).equals("REGISTER TOSEND")))
         {
           outToClient.writeBytes("ERROR 101 No user registered1\n\n");
           return;
         }
         
         if(clientSentence.split(" ").length!=4)
         {
           outToClient.writeBytes("ERROR 101 No user registered2\n\n");
           return;
         }
         String dummy=inFromClient.readLine();
         String[] split_input = clientSentence.split(" ",4);
         String username = split_input[2];
         String publicKeyString = split_input[3];
         Quadruple userSocket = new Quadruple (connectionSocket, inFromClient, outToClient, publicKeyString);

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

           String fetchReq = inFromClient.readLine();
           System.out.println(fetchReq);

           if(fetchReq.equals("DEREGISTER"))
           {
              Quadruple sender_receive_socket = server.userReceiveTable.get(username);
              sender_receive_socket.getValue2().writeBytes("DEREGISTER"+"\n\n");
              server.userReceiveTable.remove(username);
              server.userSendTable.remove(username);

              String dummy7 = inFromClient.readLine();
              break;
           }

           if(fetchReq.split(" ").length!=2 || !(fetchReq.split(" ",2)[0].equals("FETCHKEY")))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete4\n\n");
              continue;
           }

           if (! server.userReceiveTable.containsKey(fetchReq.split(" ",2)[1]))
           {
              outToClient.writeBytes("ERROR 102 Unable to send\n\n");
              System.out.println("ERROR 102 Unable to send");
              String dummy4 = inFromClient.readLine();
              continue;
           }

           Quadruple recipientSocket_fetching = server.userReceiveTable.get(fetchReq.split(" ",2)[1]);

           outToClient.writeBytes("KEY "+fetchReq.split(" ",2)[1]+" "+recipientSocket_fetching.getValue3()+"\n\n");




           String dummy0 = inFromClient.readLine();

           String recipient = inFromClient.readLine();

           if(recipient.split(" ").length!=2 || !(recipient.split(" ",2)[0].equals("SEND")))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete1\n\n");
              continue;
           }


           System.out.println(recipient);


           String content_length = inFromClient.readLine();
           if(content_length.split(" ").length!=2 || !(content_length.split(" ",2)[0].equals("Content-length:")))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete2\n\n");
              continue;
           }



           dummy = inFromClient.readLine();
           if(!dummy.equals(""))
           {
              outToClient.writeBytes("ERROR 103 Header Incomplete3\n\n");
              continue;
           }
          
           String recipentUsername = recipient.split(" ",2)[1];
           String message="";
           int count = Integer.parseInt(content_length.split(": ",2)[1]);
           int value=0;
           while(count>0)
           {
               message+=(char)inFromClient.read();
               count--;
           }


           if (! server.userReceiveTable.containsKey(recipentUsername))
           {
              outToClient.writeBytes("ERROR 102 Unable to send\n\n");
           }
           else
           {
               Quadruple recipientSocket = server.userReceiveTable.get(recipentUsername);

               DataOutputStream outToClient_recipient =recipientSocket.getValue2();
               BufferedReader inFromClient_recipient = recipientSocket.getValue1();

               outToClient_recipient.writeBytes("FORWARD "+username+"\n"+content_length +"\n\n"+ message);

               String ack_reipient = inFromClient_recipient.readLine();


               if(ack_reipient.equals("RECEIVED "+username))
               {
  
                    outToClient.writeBytes("SENT " + recipentUsername +"\n\n");
                    String dummy3 = inFromClient_recipient.readLine(); // \n from the recipient after received ack
               }
               else
                   outToClient.writeBytes("ERROR 102 Unable to send\n\n");
            }
        }
      }
	    catch(Exception e)  {
		        try
            {
			           connectionSocket.close();
		        } catch(Exception ee) { }
	      }
    }
}
