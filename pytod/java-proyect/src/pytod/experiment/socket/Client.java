package pytod.experiment.socket;

import java.io.*;
import java.net.*;
import hep.io.xdr.XDROutputStream;

class Client {
    static final String HOST = "localhost";
    static final int PUERTO = 5000;
    public Client( )
    {
        try
        {
                Socket skCliente = new Socket( HOST , PUERTO );
                XDROutputStream aux = new XDROutputStream(skCliente.getOutputStream());
                aux.writeString("a");
                aux.writeInt(2);
                //DataInputStream flujo = new DataInputStream( aux );
                //System.out.println( flujo.readUTF() );
                skCliente.close();
         }
        catch( Exception e ) 
        {
                System.out.println( e.getMessage() );
         }
    }

    public static void main( String[] arg ) {
        new Client();
    }
}

