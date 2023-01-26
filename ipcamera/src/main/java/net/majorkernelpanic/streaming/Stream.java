package net.majorkernelpanic.streaming;

import java.io.IOException;
import java.net.InetAddress;


public interface Stream {

    /**
     * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
     * to apply your configuration of the stream.
     */
    void configure() throws IllegalStateException, IOException;

    /**
     * Starts the stream.
     * This method can only be called after {@link Stream#configure()}.
     */
    void start() throws IllegalStateException, IOException;

    /**
     * Stops the stream.
     */
    void stop();

    /**
     * Sets the Time To Live of packets sent over the network.
     *
     * @param ttl The time to live
     * @throws IOException
     */
    void setTimeToLive(int ttl) throws IOException;

    /**
     * Sets the destination ip address of the stream.
     *
     * @param dest The destination address of the stream
     */
    void setDestinationAddress(InetAddress dest);

    /**
     * Sets the destination ports of the stream.
     * If an odd number is supplied for the destination port then the next
     * lower even number will be used for RTP and it will be used for RTCP.
     * If an even number is supplied, it will be used for RTP and the next odd
     * number will be used for RTCP.
     *
     * @param dport The destination port
     */
    void setDestinationPorts(int dport);

    /**
     * Sets the destination ports of the stream.
     *
     * @param rtpPort  Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    void setDestinationPorts(int rtpPort, int rtcpPort);

    /**
     * Returns a pair of source ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    int[] getLocalPorts();

    /**
     * Returns a pair of destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    int[] getDestinationPorts();


    /**
     * Returns the SSRC of the underlying {@link net.majorkernelpanic.streaming.rtp.RtpSocket}.
     *
     * @return the SSRC of the stream.
     */
    int getSSRC();

    /**
     * Returns an approximation of the bit rate consumed by the stream in bit per seconde.
     */
    long getBitrate();

    /**
     * Returns a description of the stream using SDP.
     * This method can only be called after {@link Stream#configure()}.
     *
     * @throws IllegalStateException Thrown when {@link Stream#configure()} wa not called.
     */
    String getSessionDescription() throws IllegalStateException;

    boolean isStreaming();

}
