// const websocket = new WebSocket('wss://192.168.0.102:8443/socket');
//
// let peerConnection;
// let localStream;
// let remoteStream;
//
// const localVideo = document.getElementById('localVideo');
// const remoteVideo = document.getElementById('remoteVideo');
//
// websocket.onopen = function () {
//     const configuration = null;
//
//     peerConnection = new RTCPeerConnection(configuration);
//
//     const constraints = {audio: true, video: {width: 100, height: 70}};
//     navigator.mediaDevices.getUserMedia(constraints)
//         .then(stream => {
//             localStream = stream;
//             localVideo.srcObject = stream;
//
//             stream.getTracks().forEach(track => {
//                 peerConnection.addTrack(track, stream);
//             });
//
//             localVideo.onloadedmetadata = () => {
//                 localVideo.play();
//             };
//         })
//         .catch(error => {
//             console.error('Error accessing media devices.', error);
//         });
//
//     peerConnection.onicecandidate = function (event) {
//         if (event.candidate) {
//             send({
//                 event: "candidate",
//                 data: event.candidate
//             });
//         }
//     };
//
//     peerConnection.ontrack = function (event) {
//         if (!remoteStream) {
//             remoteStream = new MediaStream();
//             remoteVideo.srcObject = remoteStream;
//         }
//         remoteStream.addTrack(event.track);
//
//         remoteVideo.onloadedmetadata = () => {
//             remoteVideo.play();
//         };
//     };
// };
//
// websocket.onmessage = function (msg) {
//     const content = JSON.parse(msg.data);
//     const data = content.data;
//     switch (content.event) {
//         case "offer":
//             handleOffer(data);
//             break;
//         case "answer":
//             handleAnswer(data);
//             break;
//         case "candidate":
//             handleCandidate(data);
//             break;
//         default:
//             break;
//     }
// };
//
// function handleOffer(offer) {
//     peerConnection.setRemoteDescription(new RTCSessionDescription(offer))
//         .then(() => peerConnection.createAnswer())
//         .then(answer => peerConnection.setLocalDescription(answer))
//         .then(() => send({event: "answer", data: peerConnection.localDescription}))
//         .catch(error => console.error('Error handling offer:', error));
// }
//
// function handleAnswer(answer) {
//     peerConnection.setRemoteDescription(new RTCSessionDescription(answer))
//         .then(() => console.log("Connection established successfully!"));
// }
//
// function handleCandidate(candidate) {
//     peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
//         .then(() => console.log("Candidate added successfully!"));
// }
//
// function send(message) {
//     websocket.send(JSON.stringify(message));
// }
//
// function makeCall() {
//     peerConnection.createOffer()
//         .then(offer => peerConnection.setLocalDescription(offer))
//         .then(() => send({event: "offer", data: peerConnection.localDescription}))
//         .catch(error => console.error('Error creating offer:', error));
// }