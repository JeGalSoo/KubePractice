import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

interface ChatMessage {
  id?: number;
  chatRoomId: number;
  senderId?: number;
  senderEmail?: string;
  senderName: string;
  content: string;
  sentAt: string;
  type?: string;
}

export default function ChatRoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [showInvite, setShowInvite] = useState(false);
  const [wsStatus, setWsStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const [loadingHistory, setLoadingHistory] = useState(true);
  const ws = useRef<WebSocket | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const currentUserObj = localStorage.getItem('user');
  const currentUser = currentUserObj ? JSON.parse(currentUserObj) : null;
  const currentName = currentUser?.name || '';
  const currentEmail = currentUser?.email || '';

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const connectWebSocket = useCallback(() => {
    const token = localStorage.getItem('token');
    if (!token || !roomId) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/chat?roomId=${roomId}&token=${token}`;

    if (ws.current && ws.current.readyState !== WebSocket.CLOSED) {
      ws.current.close();
    }

    const socket = new WebSocket(wsUrl);
    setWsStatus('connecting');

    socket.onopen = () => {
      console.log('Connected to WebFlux Chat Server');
      setWsStatus('connected');
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current);
        reconnectTimer.current = null;
      }
    };

    socket.onmessage = (event) => {
      try {
        const newMsg: ChatMessage = JSON.parse(event.data);
        // NOTIFICATION 타입은 다른 방 알림 - 이 채팅창에는 표시 안 함
        if (newMsg.type === 'NOTIFICATION') return;
        if (newMsg.chatRoomId === parseInt(roomId!)) {
          setMessages(prev => [...prev, {
            ...newMsg,
            sentAt: newMsg.sentAt || new Date().toISOString(),
          }]);
        }
      } catch (e) {
        console.error('Invalid message format', e);
      }
    };

    socket.onerror = (err) => {
      console.error('WebSocket Error:', err);
      setWsStatus('disconnected');
    };

    socket.onclose = () => {
      console.log('Disconnected from WebFlux Chat Server');
      setWsStatus('disconnected');
      // 5초 후 자동 재연결
      reconnectTimer.current = setTimeout(() => {
        connectWebSocket();
      }, 5000);
    };

    ws.current = socket;
  }, [roomId]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    // 히스토리 로드 - 오름차순으로 받아서 바로 표시
    setLoadingHistory(true);
    axios.get(`/api/chat/rooms/${roomId}/messages?size=50&sort=sentAt,asc`, {
      headers: { Authorization: `Bearer ${token}` }
    }).then(res => {
      const contentArray = Array.isArray(res.data) ? res.data : (res.data.content || []);
      setMessages(contentArray.map((m: ChatMessage) => ({
        ...m,
        sentAt: m.sentAt ? String(m.sentAt) : '',
      })));
    }).catch(console.error)
      .finally(() => setLoadingHistory(false));

    // WebSocket 연결
    connectWebSocket();

    // 채팅방 참여 등록
    axios.post(`/api/chat/rooms/${roomId}/join`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    }).catch(console.error);

    return () => {
      if (ws.current) ws.current.close();
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
    };
  }, [roomId, navigate, connectWebSocket]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    if (!ws.current || ws.current.readyState !== WebSocket.OPEN) {
      alert('채팅 서버에 연결 중입니다. 잠시 후 다시 시도해주세요.');
      return;
    }

    const payload = {
      chatRoomId: parseInt(roomId!),
      senderEmail: currentEmail,
      senderName: currentName,
      content: inputText,
      sentAt: new Date().toISOString(),
      type: 'MESSAGE',
    };

    ws.current.send(JSON.stringify(payload));
    setInputText('');
  };

  const inviteUser = async () => {
    if (!inviteEmail) return;
    try {
      await axios.post(`/api/chat/rooms/${roomId}/invite`, { email: inviteEmail }, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      alert('초대되었습니다.');
      setInviteEmail('');
      setShowInvite(false);
    } catch (err) {
      console.error(err);
      alert('초대에 실패했습니다. 이메일을 확인해주세요.');
    }
  };

  const formatTime = (sentAt: string) => {
    if (!sentAt) return '';
    try {
      return new Date(sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 h-[80vh] flex flex-col">
      <div className="flex flex-col bg-blue-600 text-white p-4 rounded-t-lg">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold">채팅방 #{roomId}</h1>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
              wsStatus === 'connected' ? 'bg-green-400 text-green-900' :
              wsStatus === 'connecting' ? 'bg-yellow-300 text-yellow-900' :
              'bg-red-400 text-red-900'
            }`}>
              {wsStatus === 'connected' ? '● 연결됨' : wsStatus === 'connecting' ? '○ 연결 중...' : '● 연결 끊김 (재시도 중)'}
            </span>
          </div>
          <div className="flex gap-4">
            <button onClick={() => setShowInvite(!showInvite)} className="hover:underline text-sm bg-blue-700 px-3 py-1 rounded">
              {showInvite ? '닫기' : '초대하기'}
            </button>
            <button onClick={() => navigate('/chat')} className="hover:underline text-sm">← 목록으로</button>
          </div>
        </div>

        {showInvite && (
          <div className="mt-4 flex gap-2">
            <input
              type="email"
              className="px-3 py-1 text-black rounded text-sm w-64"
              placeholder="초대할 사용자 이메일"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
            />
            <button onClick={inviteUser} className="bg-white text-blue-600 px-4 py-1 rounded text-sm font-bold hover:bg-gray-100 transition">
              초대
            </button>
          </div>
        )}
      </div>

      <div className="flex-grow bg-gray-50 p-4 overflow-y-auto border-x border-gray-200">
        {loadingHistory ? (
          <div className="flex justify-center items-center h-full text-gray-400">메시지를 불러오는 중...</div>
        ) : (
          <div className="space-y-4">
            {messages.length === 0 && (
              <div className="text-center text-gray-400 py-10">아직 메시지가 없습니다. 첫 메시지를 보내보세요!</div>
            )}
            {messages.map((msg, idx) => {
              // senderEmail이 있으면 email로 판별, 없으면 name으로 fallback
              const isMe = msg.senderEmail ? msg.senderEmail === currentEmail : msg.senderName === currentName;
              return (
                <div key={msg.id ?? idx} className={`flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
                  {!isMe && <span className="text-xs text-gray-500 mb-1">{msg.senderName}</span>}
                  <div className={`px-4 py-2 rounded-lg max-w-[70%] break-words ${isMe ? 'bg-amber-300 text-black' : 'bg-white border text-gray-800'}`}>
                    {msg.content}
                  </div>
                  <span className="text-[10px] text-gray-400 mt-1">
                    {formatTime(msg.sentAt)}
                  </span>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      <div className="bg-white border-x border-b border-gray-200 p-4 rounded-b-lg">
        <form onSubmit={sendMessage} className="flex gap-2">
          <input
            type="text"
            className="flex-grow border rounded-full px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-400"
            placeholder={wsStatus === 'connected' ? '메시지를 입력하세요...' : '서버에 연결 중...'}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={wsStatus !== 'connected'}
          />
          <button
            type="submit"
            disabled={wsStatus !== 'connected'}
            className="bg-blue-600 text-white px-6 py-2 rounded-full hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            전송
          </button>
        </form>
      </div>
    </div>
  );
}
