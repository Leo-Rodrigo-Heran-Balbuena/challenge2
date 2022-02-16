package my_protocol;

import framework.IRDTProtocol;
import framework.Utils;

import java.util.Arrays;

/**
 * @version 10-07-2019
 *
 * Copyright University of Twente,  2013-2019
 *
 **************************************************************************
 *                            Copyright notice                            *
 *                                                                        *
 *            This file may  ONLY  be distributed UNMODIFIED.             *
 * In particular, a correct solution to the challenge must NOT be posted  *
 * in public places, to preserve the learning effect for future students. *
 **************************************************************************
 */
public class MyProtocol extends IRDTProtocol {

    // change the following as you wish:
    static final int HEADERSIZE = 1;   // number of header bytes in each packet
    static final int DATASIZE = 128;   // max. number of user data bytes in each packet

    @Override
    public void sender() {

        Integer[] fileContents = Utils.getFileContents(getFileID());
        int packetLast = (int) Math.ceil(100 + ((double) fileContents.length / (double) DATASIZE));

        int counter = 0;
        int filepointer = 0;

        while (filepointer < fileContents.length) {
            System.out.println("Sending...");

            int datalen = Math.min(DATASIZE, fileContents.length - filepointer);

            Integer[] pkt = new Integer[HEADERSIZE + datalen];

            pkt[0] = 100 + counter;

            if (counter == packetLast) {
                pkt[0] = 1;
            }

            System.arraycopy(fileContents, filepointer, pkt, HEADERSIZE, datalen);

            getNetworkLayer().sendPacket(pkt);

            System.out.println("Sent one packet with header = " + pkt[0]);

            System.out.println(pkt);
            filepointer += DATASIZE;

            boolean acknowledegement = false;

            while (!acknowledegement) {

                Utils.Timeout.SetTimeout(10000, this, pkt[0]);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Integer[] ack = getNetworkLayer().receivePacket();

                if (ack != null) {
                    System.out.println(ack[0]);

                    if (ack[0] == pkt[0]) {
                        acknowledegement = true;
                        counter++;
                    } else {
                        getNetworkLayer().sendPacket(pkt);
                        System.out.println("Packet: " + pkt[0] + " is being resent");
                    }
                }
            }
        }
    }

    @Override
    public void TimeoutElapsed(Object tag) {
        int z = (Integer) tag;
        // handle expiration of the timeout:
        System.out.println("Timer expired with tag = "+z);
    }

    @Override
    public Integer[] receiver() {
        System.out.println("Receiving...");

        // create the array that will contain the file contents
        // note: we don't know yet how large the file will be, so the easiest (but not most efficient)
        //   is to reallocate the array every time we find out there's more data
        Integer[] fileContents = new Integer[0];

        // loop until we are done receiving the file
        boolean stop = false;
        while (!stop) {

            // try to receive a packet from the network layer
            Integer[] packet = getNetworkLayer().receivePacket();

            // if we indeed received a packet
            if (packet != null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // tell the user
                System.out.println("Received packet, length = " + packet.length + "  first byte = "+packet[0] );

                packet = new Integer[] {packet[1]};

                getNetworkLayer().sendPacket(packet);

                System.out.println("Ack sent");

                // append the packet's data part (excluding the header) to the fileContents array, first making it larger
                int oldlength = fileContents.length;
                int datalen = packet.length - HEADERSIZE;
                fileContents = Arrays.copyOf(fileContents, oldlength +  datalen);
                System.arraycopy(packet, HEADERSIZE, fileContents, oldlength, datalen);

                if (packet[0] == 1) {
                    stop =  true;
                }
            } else {
                // wait ~10ms (or however long the OS makes us wait) before trying again
                try {
                    Thread.sleep(1000);
                    if (packet == null) {
                        packet = new Integer[] {0};
                    }
                    getNetworkLayer().sendPacket(packet);
                } catch (InterruptedException e) {
                    stop = true;
                }
            }
        }

        // return the output file
        return fileContents;
    }
}
