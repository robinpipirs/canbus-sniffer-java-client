package dao;



import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robinpipirs on 16/03/16.
 */
public class SerialCommunicationService implements Runnable {

    private InputStream in;
    private OutputStream sos;
    private SerialPort comPort;
    private String comPortName;
    private int baudRate;
    private String canbusString = "";
    private String[] sendMessage = {"01", "20", "80", "35", "42", "9D", "20", "59", "60", "00", "AA"};
    private boolean sendMessageNow = false;
    boolean running;
    private List<CommunicationObserver> observers = new ArrayList<>();
    private CanbusMessage message;
    private CanbusMessage canbusMessage;
    private CanbusLogFileParser canbusLogFileParser = new CanbusLogFileParser();

    SerialCommunicationService(String portName, int baudRate)
    {
    		comPortName = portName;
    		this.baudRate = baudRate;
    }

    public OutputStream getOutPutStream(){
        return sos;
    }

    public void setWriteMessage(String[] sendMessage){
        this.sendMessage = sendMessage;

        for (int i = 0; i < sendMessage.length; i++){
            System.out.print(sendMessage[i]+" ");
        }
        System.out.println("");
        sendMessageNow = true;
    }

    public CanbusMessage getMessage() {
        return message;
    }

    public void setMessage(CanbusMessage message) {
        this.message = message;
        notifyAllObservers();
     }

    public void attach(CommunicationObserver observer){
        observers.add(observer);
    }

    public void notifyAllObservers(){
        for (CommunicationObserver observer : observers) {
            observer.update();
        }
    }


    @Override
    public void run() {
        running = true;
        comPort = SerialPort.getCommPort(comPortName);
        comPort.setBaudRate(baudRate);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        in = comPort.getInputStream();
        sos = comPort.getOutputStream();
        try
        {
            while(running){
                char c = (char)in.read();

                if (c != '\n')
                {
                    canbusString = canbusString + String.valueOf(c);
                }
                else {
                    //System.out.println("debug: "+canbusString);
                    canbusMessage = canbusLogFileParser.ParseCanBusStringFromInputSerial(canbusString);
                    //System.out.println("parsed message: " +canbusMessage);
                    if ((canbusMessage != null) && !canbusMessage.getId().equals("TO")){
                        setMessage(canbusMessage);
                    }
                    //System.out.println("can: "+canbusString);
                    canbusString = "";
                }
                //Raw: 01 20 80 35 42 9D 20 59 60 00 (AA 0A) = 11 bytes

            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            running = false;
        }
        comPort.closePort();
    }

}
//sos.write(new byte[]{
//        (byte)(Integer.parseInt("01",16) & 0xff),
//        (byte)(Integer.parseInt("20",16) & 0xff),
//        (byte)(Integer.parseInt("80",16) & 0xff),
//        (byte)(Integer.parseInt("35",16) & 0xff),
//        (byte)(Integer.parseInt("42",16) & 0xff),
//        (byte)(Integer.parseInt("9D",16) & 0xff),
//        (byte)(Integer.parseInt("20",16) & 0xff),
//        (byte)(Integer.parseInt("59",16) & 0xff),
//        (byte)(Integer.parseInt("60",16) & 0xff),
//        (byte)(Integer.parseInt("00",16) & 0xff),
//        (byte)(Integer.parseInt("AA",16) & 0xff),
//        (byte)(Integer.parseInt("0A",16) & 0xff)
//        });