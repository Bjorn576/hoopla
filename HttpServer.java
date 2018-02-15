/*Write your name and student ID here */
/*You must make sure your code is compliant with https://www.ietf.org/rfc/rfc1945.txt*/


/*To compile and execute this code in linux run:
   javac HttpServer.java
   java HttpServer                         */


import java.io.* ;
import java.net.* ;
import java.util.* ;
/*These are the minimum imports you need, if you add additional ones make sure they are not doing all the work for you!*/


public final class HttpServer
{
    public static void main(String argv[])
    {
        // Set the port number.
        int port = 6789;
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);//Create listener
            int numOpen = 0;

            while(true) {
                System.out.println("Still waiting!");
                Socket clientSocket = serverSocket.accept(); //Accept a connection
                numOpen++;
                HttpRequest request = new HttpRequest(clientSocket, numOpen);//Create a connection handler
                Thread service = new Thread(request);
                service.start();
            }
        }
        catch (IOException e) {
            System.err.println("Server socket failure: " + e.getMessage());
        }
    }
}

final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    private Socket clientSocket;
    // Track thread number for testing purposes
    private int num;

    HttpRequest(Socket clientSocket, int num)
    {
            this.clientSocket = clientSocket;
            this.num = num;
    }

    //The client handling code will be written here
    public void run() {
        processRequest();
        //System.out.println(String.format("Execution %s finished", num));
    }

    private void processRequest() {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println(String.format("Socket %s Connected", num));

            String requestLine;
            if ((requestLine = in.readLine()).isEmpty()) {
                throw new IOException("Request is empty");
            }
            System.out.println(String.format("Request is: \n%s", requestLine));
            StringTokenizer tokens = new StringTokenizer(requestLine);
            tokens.nextToken();
            String fileName = "." + tokens.nextToken();
            String header;

            while (!(header = in.readLine()).isEmpty()) {
                System.out.println(header);
            }

            FileInputStream fis = null;
            boolean fileExists = true;
            boolean badRequest = false;
            try {
                fis = new FileInputStream(fileName);
            } catch (FileNotFoundException e) {
                fileExists = false;
            }
            if (!requestLine.matches("GET /.+\\..+ HTTP/1\\.\\d")){
                badRequest = true;
            }

            String statusLine = "HTTP/1.0 ";
            String contentTypeLine = "Content-Type: ";
            String respBody = null;
            if (fileExists && !badRequest) {
                statusLine += "200 OK";
                contentTypeLine += contentType(fileName);
            } else if (badRequest){
                statusLine += "400 Bad Request";
                contentTypeLine += "text/plain";
                respBody = "Request should be 'GET /<file> HTTP/1.0\n";

            } else {
                statusLine += "404 Not Found";
                contentTypeLine += "text/html";
                respBody = "<HTML>" +
                        "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                        "<BODY>Not Found</BODY></HTML>";
            }

            writeHeader(out, statusLine, contentTypeLine);
            if (fileExists && !badRequest) {
                // Send the file pointed to by 'fis' to the output stream 'out'
                sendBytes(fis, out);
            } else {
                // Send a predefined string as the body
                out.writeBytes(respBody);
            }

            out.close();
            in.close();
            clientSocket.close();
        } catch (IOException io) {
            System.err.println(io.getMessage());
        } catch (Exception e) {
            if (out != null) {
                writeHeader(out, "HTTP/1.0 500 Internal Server Error", "Content-Type: text/plain");
            }
        }
    }

    private void writeHeader(DataOutputStream out, String statusLine, String contentTypeLine) {
        // Add CRLF at the end of the status line to indicate the line is ended
        statusLine += CRLF;
        try {
            out.writeBytes(statusLine);
            out.writeBytes(contentTypeLine + CRLF);
            out.writeBytes(CRLF);
        } catch (IOException io) {
            System.err.println(String.format("Server could not send response:\n%s\n(%s)", statusLine, io.getMessage()));
        }
    }

    private static String contentType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            return "text/html";
        }

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".jpe")) {
            return "image/jpeg";
        }

        if (filename.endsWith(".bmp")) {
            return "image/bmp";
        }

        if (filename.endsWith(".gif")) {
            return "image/gif";
        }

        if (filename.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }

    private static void sendBytes(FileInputStream fis, DataOutputStream out) throws IOException{
        byte[] outBuf = new byte[1024];
        int bytes;
        while ((bytes = fis.read(outBuf)) != -1) {
            out.write(outBuf, 0, bytes);
        }
        out.flush();

    }
}


