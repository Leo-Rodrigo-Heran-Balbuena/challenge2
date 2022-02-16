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

    int counter = 0;

    @Override
    public void sender() {
        System.out.println("Sending...");

        Integer[] fileContents = Utils.getFileContents(getFileID());

        int filePointer = 0;

        int datalen = Math.min(DATASIZE, fileContents.length - filePointer);

        Integer[] pkt = new Integer[HEADERSIZE + datalen];

        pkt[0] = 123 + counter;

        System.arraycopy(fileContents, filePointer, pkt, HEADERSIZE, datalen);

        getNetworkLayer().sendPacket(pkt);

        System.out.println("Sent one packet with header = "+pkt[0]);

        framework.Utils.Timeout.SetTimeout(1000, this, 28);

        boolean stop = false;
        while (!stop) {
            try {
                Thread.sleep(100);
                Integer[] ack = getNetworkLayer().receivePacket();

                if (ack != pkt) {
                    getNetworkLayer().sendPacket(pkt);
                }
            } catch (InterruptedException e) {
                stop = true;
                counter++;
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

                // tell the user
                System.out.println("Received packet, length = " +packet.length+ "  first byte = "+packet[0] );

                // append the packet's data part (excluding the header) to the fileContents array, first making it larger
                int oldlength = fileContents.length;
                int datalen = packet.length - HEADERSIZE;
                fileContents = Arrays.copyOf(fileContents, oldlength+datalen);
                System.arraycopy(packet, HEADERSIZE, fileContents, oldlength, datalen);

                // and let's just hope the file is now complete
                stop = true;

            }else{
                // wait ~10ms (or however long the OS makes us wait) before trying again
                try {
                    getNetworkLayer().sendPacket(packet);
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    stop = true;
                }
            }
        }

        // return the output file
        return fileContents;
    }
}
