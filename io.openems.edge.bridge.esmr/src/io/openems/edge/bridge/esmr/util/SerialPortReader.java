package io.openems.edge.bridge.esmr.util;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.util.Collection;

public class SerialPortReader implements SerialPortEventListener, AutoCloseable {

    private final String portIdentifier; // Port identifier for serial, i.e. /dev/ttyUSB0
    private int timeOut = 2000;
    private int baudRate = 115200;
    private DataBits dataBits = DataBits.DATABITS_8;
    private StopBits stopBits = StopBits.STOPBITS_1;
    private Parity parity = Parity.NONE;
    private ReadUTF8RecordStream reader;
    private SerialPort serialPort;

    private Collection<Notifiable> notifiables;

    public SerialPortReader(String portIdentifier) {
        this.portIdentifier = portIdentifier;
    }

    public SerialPortReader(String portIdentifier, int baudRate) {
        this(portIdentifier);
        this.baudRate = baudRate;
    }

    public SerialPortReader(String portIdentifier, int baudRate, DataBits dataBits, StopBits stopBits, Parity parity) {
        this(portIdentifier, baudRate);
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public SerialPortReader(String portIdentifier, int baudRate, DataBits dataBits, StopBits stopBits, Parity parity, int timeOut) {
        this(portIdentifier, baudRate, dataBits, stopBits, parity);
        this.timeOut = timeOut;
    }

    public void addNotifiable(Notifiable notifiable) {
        this.notifiables.add(notifiable);
    }

    public void removeNotifiable(Notifiable notifiable) {
        this.notifiables.remove(notifiable);
    }

    public boolean initConnection() {
        try {
            CommPortIdentifier port = CommPortIdentifier.getPortIdentifier(portIdentifier);
            this.serialPort = (SerialPort) port.open(this.getClass().getName(), timeOut);
            serialPort.setSerialPortParams(baudRate, dataBits.getOldValue(), stopBits.getOldValue(), parity.getOldValue());

            var inputStream = serialPort.getInputStream();
            this.reader = new ReadUTF8RecordStream(inputStream, "\r\n![0-9A-F]{4}\r\n");

            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    @Override
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                var response = this.reader.read();
                for (Notifiable notifiable : notifiables) {
                    notifiable.onEvent(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
