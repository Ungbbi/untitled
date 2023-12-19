package Server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final int PORT = 3000;
    private static final int TIMEOUT = 3000;
    private static final int INITIAL_WINDOW_SIZE = 1;

    public static void main(String[]args){
        try{
            DatagramSocket socket = new DatagramSocket();

            int cwnd = INITIAL_WINDOW_SIZE;
            int base = 0;
            int nextSeqNum = 0;
            int dupAckCnt = 0;
            int lastAckNum = 0;
            int threshold = 8;

            while(true){
                for(int i = base ; i <Math.min(nextSeqNum,base + cwnd); i++){
                    byte[] sendData = ("패킷 "+i).getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("local host"),PORT);
                    socket.send(sendPacket);
                }

                byte[] recvData = new byte[1024];
                DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);

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
                        }
                    }
                    else{                          // 중복ACK가 아닐 경우
                        // TODO 새로운 ACK일경우
                        lastAckNum = recvAckNum;
                        dupAckCnt = 0;
                        base = nextSeqNum;
                        System.out.println("ACK"+lastAckNum+"수신");
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

