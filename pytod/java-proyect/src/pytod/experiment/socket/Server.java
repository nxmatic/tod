package pytod.experiment.socket;

import java.io.IOException;
import hep.io.xdr.XDRInputStream;
import java.io.* ;
import java.net.* ;

class Server {
      static final int PUERTO = 5000;
      public Server( )
      {
            try 
            {
                ServerSocket skServidor = new ServerSocket(PUERTO);
                System.out.println("Escucho el puerto " + PUERTO );
                for ( int numCli = 0; numCli < 3; numCli++) 
                {
                    Socket skCliente = skServidor.accept(); // Crea objeto
                    System.out.println("Sirvo al cliente " + numCli);
                    XDRInputStream theStream = new XDRInputStream(skCliente.getInputStream());
                    String palabra = theStream.readString();
                    //DataOutputStream flujo = aux;
                    //flujo.writeUTF( "Hola cliente " + numCli );
                    System.out.println(palabra);
                    System.out.println(theStream.read());
                    
                    skCliente.close();
                }
                System.out.println("Demasiados clientes por hoy");
            }
            catch( Exception e )
            {
                System.out.println( e.getMessage() );
            }
    }

    public static void main( String[] arg )
    {
        new Server();
    }
}

