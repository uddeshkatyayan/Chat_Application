import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

class TCPClient {

    public static void main(String args[]) throws Exception
    {
        String sentence;
        String modifiedSentence;
        if(args.length != 2)
        {
            System.out.println("Enter username and server ip correctly");
            return;
        }
				String username = args[0];
				String server_ip = args[1];

        BufferedReader inFromUser =
          new BufferedReader(new InputStreamReader(System.in));

        CryptographyExample crypto = new CryptographyExample();

        KeyPair keyPair = crypto.generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();

        String publicKeyString = java.util.Base64.getEncoder().encodeToString(publicKey);
        // System.out.println(publicKeyString);

        Socket sendSocket = new Socket(server_ip, 6789);
				Socket receiveSocket = new Socket(server_ip, 6789);

        DataOutputStream outToServer_SEND = new DataOutputStream(sendSocket.getOutputStream());
				DataOutputStream outToServer_RECEIVE = new DataOutputStream(receiveSocket.getOutputStream());


        BufferedReader inFromServer_SEND =  new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
				BufferedReader inFromServer_RECEIVE =  new BufferedReader(new InputStreamReader(receiveSocket.getInputStream()));


				outToServer_SEND.writeBytes("REGISTER TOSEND " + username +" "+ publicKeyString +"\n\n"); //registration message_send
				String server_ack_SEND = inFromServer_SEND.readLine();
				System.out.println(server_ack_SEND);
        String dummy1 = inFromServer_SEND.readLine();

				outToServer_RECEIVE.writeBytes("REGISTER TORECV "+ username +" "+ publicKeyString+ "\n\n");
				String server_ack_RECEIVE = inFromServer_RECEIVE.readLine();
				System.out.println(server_ack_RECEIVE);
        String dummy2 = inFromServer_RECEIVE.readLine();


				if(server_ack_SEND.substring(0,3).equalsIgnoreCase("ERR") || server_ack_RECEIVE.substring(0,3).equalsIgnoreCase("ERR"))
				{
					System.out.println(server_ack_SEND);
          System.out.println(server_ack_RECEIVE);
					return;
				}

				SendThread send_thread = new SendThread(sendSocket,inFromUser,outToServer_SEND, inFromServer_SEND, privateKey);

				ReceiveThread receive_thread = new ReceiveThread(receiveSocket, inFromServer_RECEIVE, outToServer_RECEIVE, privateKey );

				Thread send_thread_start = new Thread(send_thread);
				Thread receive_thread_start = new Thread(receive_thread);

				send_thread_start.start();
				receive_thread_start.start();






        // while(true) {
				//
        //      sentence = inFromUser.readLine();
				//
        //      outToServer.writeBytes(sentence + '\n');
				//
        //      modifiedSentence = inFromServer.readLine();
				//
        //      System.out.println("FROM SERVER: " + modifiedSentence);
				//
        // }

//        clientSocket.close();

    }
}


class SendThread implements Runnable {
	String input_sentence;
	Socket sendSocket;
	DataOutputStream outToServer;
	BufferedReader inFromUser;
	BufferedReader inFromServer;
  byte[] privateKey;

	SendThread(Socket sc, BufferedReader ifu, DataOutputStream ots, BufferedReader ifs, byte[] pk )
	{
		sendSocket=sc;
		inFromUser=ifu;
		outToServer=ots;
		inFromServer=ifs;
    privateKey=pk;
	}

	public void run()
	{
		while(true)
		{
			try{
				input_sentence = inFromUser.readLine();
				String[] split_input = input_sentence.split(" ", 2);
				if(split_input.length!=2 || input_sentence.charAt(0)!='@')
				{
					System.out.println("Wrong format");
				 	continue;
				}

				// System.out.println("SEND " + split_input[0].substring(1)+"\n"+"Content-length: "+ content_length + "\n\n" + split_input[1]);
				// System.out.println("done");
        outToServer.writeBytes("FETCHKEY " + split_input[0].substring(1)+"\n\n");

        String fetch_reply = inFromServer.readLine();

        // System.out.println(fetch_reply);

        if(fetch_reply.substring(0,3).equals("ERR"))
        {
          System.out.println(fetch_reply);
          String dummy3 = inFromServer.readLine();
          continue;
        }

        String recipient_public_key = fetch_reply.split(" ",3)[2];

        String dummy4=inFromServer.readLine();

        CryptographyExample crypto = new CryptographyExample();


        byte[] encrypted_message_byte = crypto.encrypt(  java.util.Base64.getDecoder().decode(recipient_public_key) , split_input[1].getBytes() );

        String encrypted_message = java.util.Base64.getEncoder().encodeToString(encrypted_message_byte);

        // System.out.println(encrypted_message);
        // System.out.println(encrypted_message_byte);
        int content_length = encrypted_message.length();

        // SIGNATURE

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] H = md.digest(encrypted_message_byte);

        byte[] H_dash = crypto.encrypt( privateKey , hash_encrypted_message);

        String signature = java.util.Base64.getEncoder().encodeToString(H_dash);


				outToServer.writeBytes("SEND " + split_input[0].substring(1)+"\n"+ "Signature " + signature + "\n" +"Content-length: "+ content_length + "\n\n"+ encrypted_message );

				// String ack_message = inFromServer.readLine(); //for the empty line sent from the server after sending the Registered to send message.
        String ack_message = inFromServer.readLine();

				// if(ack_message.substring(0,3).equals("ERR"))
				// {
				System.out.println(ack_message);
				ack_message=inFromServer.readLine();

			}
			catch(Exception e){}
		}
	}
}

class ReceiveThread implements Runnable {
	String input_sentence;
	Socket receiveSocket;
	DataOutputStream outToServer;
	BufferedReader inFromServer;
  byte[] privateKey;

 // ReceiveThread(receiveSocket, inFromServer_RECEIVE, outToServer_RECEIVE )
	ReceiveThread(Socket rs, BufferedReader ifs, DataOutputStream ots, byte[] pk)
	{
		receiveSocket=rs;
		outToServer=ots;
		inFromServer=ifs;
    privateKey=pk;
	}

	public void run()
	{
		while(true)
		{
			try{

			String senderName;
			input_sentence = inFromServer.readLine();
			String[] split_input = input_sentence.split(" ",2);
			System.out.println("FROM: "+ split_input[1]);
			senderName = split_input[1];
			String	content_length = inFromServer.readLine();

			input_sentence=inFromServer.readLine(); //\n

			// input_sentence=inFromServer.readLine();

      String message="";
      int count = Integer.parseInt(content_length.split(": ",2)[1]);
      int value=0;
      while(count>0)
      {
          message+=(char)inFromServer.read();
          count--;
          // System.out.println(message);
      }

      // System.out.println(message);


      CryptographyExample crypto = new CryptographyExample();
      //
      //
      byte[] message_byte =  java.util.Base64.getDecoder().decode(message);
      // System.out.println(message_byte+" message_byte");
      //
      // System.out.println(privateKey);

      byte[] decrypted_message_byte =  crypto.decrypt(privateKey, message_byte ) ;
      // System.out.println(decrypted_message_byte+" decrypted_message_byte ");
      //
      String decrypted_message =  new String(decrypted_message_byte);

      System.out.println(decrypted_message);
      //

			// if(Integer.parseInt(string_len)==input_sentence.length())
			outToServer.writeBytes("RECEIVED "+ senderName+"\n\n");
			// else
      // System.out.println("Received message from "+ senderName);

			// outToServer.writeBytes("E"+ senderName+"\n\n");


		}
		catch(Exception e){}
		}
	}
}







// class TCPClient
// {
// 	public static void main(String args[])throws IOException
// 	{
// 		String sentence;
// 		String modifiedSen;
// 		InputStreamReader in = new InputStreamReader(System.in);
// 		BufferedReader inFromUser = new BufferedReader(in);
//
// 		Socket send_Socket = new Socket("localhost", 6789);
// 		Socket recieve_Socket = new Socket("localhost", 6789);
//
// 		// while(true)
// 		// {
// 			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
// 			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//
// 			//UserInterface: taking input from the terminal and forwarding the appropriate message top server
// 			Thread userInterface = new Thread(new Runnable()
//       {
//           @Override
//           public void run()
// 					{
// 							String msg = sentence.substring(sentence.indexOf(" "));
// 							String user = sentence.substring(1, sentence.indexOf(" "));
// 							outToServer.writeBytes("REGISTER TOSEND "+user+"\n");
//               while (true)
// 							{
//                   // read the message to deliver.
//                   String sentence = inFromUser.readLine();
//                   try
// 									{
// 										String fromServer = inFromServer.readLine();
// 										if(fromServer.equalsIgnoreCase("ERROR 100 Malformed "+user))
// 										{
// 											System.out.println("No such user found. \n Re-enter a valid user name");
// 											continue;
// 										}
// 										// else if(fromServer.equalsIgnoreCase("REGISTERED TOSEND "+user))
// 										// {
// 										outToServer.writeBytes("REGISTER TORECV "+user+"\n");
// 										fromServer = inFromServer.readLine();
// 										// }
// 										outToServer.writeBytes("SEND "+user+"\n");
// 										outToServer.writeBytes("Content-length: "+msg.length()+"\n");
// 										outToServer.writeBytes(msg+"\n");
//
// 										fromServer = inFromServer.readLine();
//
// 										if(fromServer.equals("SEND"))
// 											System.out.println("Message Sent");
// 										else
// 											System.out.println("ERROR, Message not delivered. Please try again");
//                     // dos.writeUTF(msg);
//                   } catch (IOException e)
//
//               }
//           }
//       });
//
// 			Thread receiverInterface =  new Thread(new Runnable()
//
// 				@Override
// 				public void run()
// 				{
//
// 				}
// 			)
//
//
//
// 			sentence =inFromUser.readLine();
// 			if(sentence.equals("\n"))
// 				break;
// 			outToServer.writeBytes(sentence+'\n');
// 			modifiedSen = inFromServer.readLine();
// 			System.out.println("FROM SERVER: " + modifiedSen);
// 		// }
//
// 		clientSocket.close();
// 	}
// }
//
//
//
//
//
//
//
// class UserInterface implements Runnable
// {
// 	String username;
// 	String message;
// 	Thread t;
// 	UserInterface(String threadname)
// 	{
// 		t=new Thread(this,threadname);
// 		t.start();
// 	}
// 	public void run()
// 	{
// 		try
// 		{
// 			BufferedReader inFromUser = new BufferedReader(InputStreamReader(System.in));
// 			sentence = inFromUser.readLine();
// 			if( ! sentence.isEmpty())
// 			{
// 				String msg = sentence.substring(sentence.indexOf(" "));
// 				String user = sentence.substring(1, sentence.indexOf(" "));
// 				this.username=user;
// 				this.message=msg;
// 			}
// 		}
// 		catch (InterruptedException e)
// 			System.out.println("thread interrupted");
// 	}
//
// 	public getUsername()
// 	{
// 		return this.username;
// 	}
//
// 	public getMessage()
// 	{
// 		return this.message;
// 	}
// // }
