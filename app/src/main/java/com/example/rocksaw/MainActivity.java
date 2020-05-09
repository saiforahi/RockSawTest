package com.example.rocksaw;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.savarese.vserv.tcpip.ICMPEchoPacket;
import org.savarese.vserv.tcpip.OctetConverter;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button= this.<Button>findViewById(R.id.ping_btn);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // click handling code
                TextView tv = findViewById(R.id.textView);
                tv.setText("working");

                final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

                try{
                    final InetAddress address = InetAddress.getByName("10.0.2.2");
                    final String hostname = address.getCanonicalHostName();
                    final String hostaddr = address.getHostAddress();
                    final int count=5;
                    // Ping programs usually use the process ID for the identifier,
                    // but we can't get it and this is only a demo.
                    final int id = 65535;
                    final Ping.Pinger ping;


                    if(address instanceof Inet6Address)
                        ping = new Ping.PingerIPv6(id);
                    else
                        ping = new Ping.Pinger(id);

                    ping.setEchoReplyListener(new Ping.EchoReplyListener() {
                        StringBuffer buffer = new StringBuffer(128);
                        public void notifyEchoReply(ICMPEchoPacket packet, byte[] data, int dataOffset, byte[] srcAddress) throws IOException
                        {
                            long end   = System.nanoTime();
                            long start = OctetConverter.octetsToLong(data, dataOffset);
                            // Note: Java and JNI overhead will be noticeable (100-200
                            // microseconds) for sub-millisecond transmission times.
                            // The first ping may even show several seconds of delay
                            // because of initial JIT compilation overhead.
                            double rtt = (double)(end - start) / 1e6;

                            buffer.setLength(0);
                            buffer.append(packet.getICMPPacketByteLength())
                                    .append(" bytes from ").append(hostname).append(" (");
                            buffer.append(InetAddress.getByAddress(srcAddress).toString());
                            buffer.append("): icmp_seq=")
                                    .append(packet.getSequenceNumber())
                                    .append(" ttl=").append(packet.getTTL()).append(" time=")
                                    .append(rtt).append(" ms");
                            System.out.println(buffer.toString());
                        }
                    });

                    System.out.println("PING " + hostname + " (" + hostaddr + ") " + ping.getRequestDataLength() + "(" + ping.getRequestPacketLength() + ") bytes of data).");
                    tv.setText("working1");
                    final CountDownLatch latch = new CountDownLatch(1);

                    executor.scheduleAtFixedRate(new Runnable() {
                        int counter = count;

                        public void run() {
                            try {
                                if(counter > 0) {
                                    ping.sendEchoRequest(address);
                                    if(counter == count)
                                        latch.countDown();
                                    --counter;
                                } else
                                    executor.shutdown();
                            } catch(IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    }, 0, 1, TimeUnit.SECONDS);

                    // We wait for first ping to be sent because Windows times out
                    // with WSAETIMEDOUT if echo request hasn't been sent first.
                    // POSIX does the right thing and just blocks on the first receive.
                    // An alternative is to bind the socket first, which should allow a
                    // receive to be performed frst on Windows.
                    latch.await();

                    for(int i = 0; i < count; ++i)
                        ping.receiveEchoReply();

                    ping.close();
                } catch(Exception e) {
                    executor.shutdown();
                    e.printStackTrace();
                }
            }
        });
    }
}
