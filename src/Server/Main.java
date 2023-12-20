package Server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final int PORT = 3000;
    private static final int TIMEOUT = 3000;
    private static final int INITIAL_WINDOW_SIZE = 1;
    private static final int MSS = 546;
    public static void main(String[]args){
        try{
            DatagramSocket socket = new DatagramSocket();

            int cwnd = INITIAL_WINDOW_SIZE;
            int base = 1;
            int nextSeqNum = 0;
            int dupAckCnt = 0;
            int lastAckNum = 0;
            int threshold = 8;
            int lastSentNum = 0;
            byte[] UserData = new byte[10000000];
            int SeqNum = 1;
            int lastbyteSent = 0;
            byte[] recvData = new byte[1024];
            while(true){
                // 패킷 생성 및 전송
                // UserData의 인덱스 [lastbyteSent~lastbyteSend+cwnd*MSS] 만큼 보냄.

                DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);

                for(int i = 1; i<=cwnd; i++){
                    byte[] sendData = new byte[MSS];
                    System.arraycopy(UserData, lastbyteSent,sendData,0,MSS);
                    // 패킷 및 ACK 번호 부여 4바이트 정수
                    sendData[0] = (byte) (SeqNum >> 24);
                    sendData[1] = (byte) (SeqNum >> 16);
                    sendData[2] = (byte) (SeqNum >> 8);
                    sendData[3] = (byte) SeqNum;

                    DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, InetAddress.getByName("local host"),PORT);
                    socket.send(sendPacket);
                    System.out.println("-----------> 패킷 "+SeqNum+" 송신");
                    lastbyteSent = lastbyteSent+MSS; // UserData의 다음 인덱스, 즉 보내야 할 인덱스로 이동시킨다.
                    SeqNum ++;  // 번호 증가

                    // 패킷 수신


                }

                // Time-Out 처리
                socket.setSoTimeout(TIMEOUT);
                boolean ackRecv = false;

                try{
                    // ACK 수신
                    socket.receive(recvPacket);
                    ackRecv = true;
                }catch(java.net.SocketTimeoutException e){
                    //TimeOut 발생 시 처리
                    // Tahoe 구현
                    threshold = cwnd/2;
                    cwnd = 1;

                    System.out.println("<<<타임아웃 사건 발생>>>");
                    System.out.println("-cwin:"+cwnd+"로 조정");
                    System.out.println("-임계값:"+threshold+"로 설정");
                }

                // 중복ACK 처리
                if(ackRecv){
                    int recvAckNum = extractAckNum(recvPacket.getData()); // ACK 번호 추출 (UDP이므로 패킷 내 데이터추출)

                    if (recvAckNum == lastAckNum){ // 중복 ACK 수신일 경우
                        dupAckCnt++;
                        if(dupAckCnt==3){
                            System.out.println("<<<3-DUP ACK 사건 발생>>>");
                            cwnd = cwnd/2 + 3 ;
                            threshold = cwnd;
                            System.out.println("-cwin:1/2로 조정->"+cwnd);
                            System.out.println("-임계값:"+threshold+"로 설정");
                        }
                    }
                    else{                          // 중복ACK가 아닐 경우
                        // TODO 새로운 ACK일경우
                        lastAckNum = recvAckNum;
                        dupAckCnt = 0;
                        base += nextSeqNum;
                        System.out.println("<---ACK"+lastAckNum+" 수신");
                        cwnd *=2;
                    }
                }
                nextSeqNum += cwnd;
            }
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private static int extractAckNum (byte[] data){
        String packetData = new String(data);
        String ackNumber = packetData.substring(6).trim(); // "패킷"을 제거한 다음 숫자
        try{
            return  Integer.parseInt(ackNumber);
        } catch (NumberFormatException e){
            //정수로 변환 불가하면
            return -1;
        }
    }
}

