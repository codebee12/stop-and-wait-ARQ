# stop-and-wait-ARQ
Implementation of Stop and Wait Protocol in JAVA 

# Features
Client File Request Format

REQUEST filename CRLF, with a space between REQUEST, filename and CRLF
Server Message Format

RDT sequence_number payload CRLF, where the payload is a byte array of 512

At the very last consignment, the message format is as follows:
RDT sequence_number payload END CRLF
Client Acknowledgement Format

ACK sequence_number CRLF
No Negative ACK.


Client sends ACK according to what it is expecting next from the Server, i.e. ACK1, ACK2, ... ACK(ServerWindowSize-1), ACK0 [assuming that Client knows about Server's window size].

