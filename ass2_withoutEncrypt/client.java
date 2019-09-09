import java.io.*;
import java.net.*;

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

        Socket sendSocket = new Socket(server_ip, 6789);
				Socket receiveSocket = new Socket(server_ip, 6789);

        DataOutputStream outToServer_SEND = new DataOutputStream(sendSocket.getOutputStream());
				DataOutputStream outToServer_RECEIVE = new DataOutputStream(receiveSocket.getOutputStream());


        BufferedReader inFromServer_SEND =  new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
				BufferedReader inFromServer_RECEIVE =  new BufferedReader(new InputStreamReader(receiveSocket.getInputStream()));


				outToServer_SEND.writeBytes("REGISTER TOSEND " + username + "\n\n"); //registration message_send
				String server_ack_SEND = inFromServer_SEND.readLine();
				System.out.println(server_ack_SEND);
        String dummy1 = inFromServer_SEND.readLine();

				outToServer_RECEIVE.writeBytes("REGISTER TORECV "+ username + "\n\n");
				String server_ack_RECEIVE = inFromServer_RECEIVE.readLine();
				System.out.println(server_ack_RECEIVE);
        String dummy2 = inFromServer_RECEIVE.readLine();


				if(server_ack_SEND.substring(0,3).equalsIgnoreCase("ERR") || server_ack_RECEIVE.substring(0,3).equalsIgnoreCase("ERR"))
				{
					System.out.println(server_ack_SEND);
          System.out.println(server_ack_RECEIVE);
					return;
				}

				SendThread send_thread = new SendThread(sendSocket,inFromUser,outToServer_SEND, inFromServer_SEND);

				ReceiveThread receive_thread = new ReceiveThread(receiveSocket, inFromServer_RECEIVE, outToServer_RECEIVE );

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

	SendThread(Socket sc, BufferedReader ifu, DataOutputStream ots, BufferedReader ifs)
	{
		sendSocket=sc;
		inFromUser=ifu;
		outToServer=ots;
		inFromServer=ifs;
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
				int content_length = split_input[1].length();

				// System.out.println("SEND " + split_input[0].substring(1)+"\n"+"Content-length: "+ content_length + "\n\n" + split_input[1]);
				// System.out.println("done");

				outToServer.writeBytes("SEND " + split_input[0].substring(1)+"\n"+"Content-length: "+ content_length + "\n\n" + split_input[1]);

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

 // ReceiveThread(receiveSocket, inFromServer_RECEIVE, outToServer_RECEIVE )
	ReceiveThread(Socket rs, BufferedReader ifs, DataOutputStream ots)
	{
		receiveSocket=rs;
		outToServer=ots;
		inFromServer=ifs;
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

			System.out.println(message);

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
