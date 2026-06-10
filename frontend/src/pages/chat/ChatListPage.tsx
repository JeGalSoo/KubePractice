import { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

interface ChatRoom {
  id: number;
  name: string;
  isGroupChat: boolean;
  createdAt: string;
}

export default function ChatListPage() {
  const [myRooms, setMyRooms] = useState<ChatRoom[]>([]);
  const [allRooms, setAllRooms] = useState<ChatRoom[]>([]);
  const [newRoomName, setNewRoomName] = useState('');
  const [targetEmail, setTargetEmail] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchMyRooms();
    fetchAllRooms();
  }, []);

  const fetchMyRooms = async () => {
    try {
      const res = await axios.get('/api/chat/rooms', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setMyRooms(res.data);
    } catch (err) {
      console.error(err);
      alert("로그인이 필요합니다.");
      navigate('/login');
    }
  };

  const fetchAllRooms = async () => {
    try {
      const res = await axios.get('/api/chat/rooms/all', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setAllRooms(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const createGroupRoom = async () => {
    try {
      if (!newRoomName) return;
      await axios.post('/api/chat/rooms', { name: newRoomName, isGroupChat: true }, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setNewRoomName('');
      fetchMyRooms();
      fetchAllRooms();
    } catch (err) {
      console.error(err);
      alert("채팅방 생성에 실패했습니다.");
    }
  };

  const startDirectChat = async () => {
    try {
      if (!targetEmail) return;
      const res = await axios.post('/api/chat/rooms/direct', { email: targetEmail }, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setTargetEmail('');
      navigate(`/chat/${res.data.id}`);
    } catch (err) {
      console.error(err);
      alert("상대방을 찾을 수 없거나 채팅방 생성에 실패했습니다.");
    }
  };

  const joinRoom = async (roomId: number) => {
    try {
      await axios.post(`/api/chat/rooms/${roomId}/join`, {}, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      navigate(`/chat/${roomId}`);
    } catch (err) {
      console.error(err);
      alert("채팅방 참여에 실패했습니다.");
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8">
      <h1 className="text-3xl font-bold mb-8">채팅</h1>
      
      {/* 1:1 채팅 시작 섹션 */}
      <div className="bg-white p-6 rounded-lg shadow mb-8">
        <h2 className="text-xl font-bold mb-4">1:1 채팅 시작하기</h2>
        <div className="flex gap-4">
          <input 
            className="border p-2 rounded flex-grow focus:outline-none focus:ring-2 focus:ring-blue-500"
            type="email" 
            value={targetEmail} 
            onChange={(e) => setTargetEmail(e.target.value)} 
            placeholder="상대방 이메일 입력" 
          />
          <button className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded transition" onClick={startDirectChat}>채팅 시작</button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* 내 채팅방 섹션 */}
        <div>
          <h2 className="text-xl font-bold mb-4">내 채팅방</h2>
          <div className="bg-white rounded-lg shadow divide-y h-96 overflow-y-auto">
            {myRooms.map(room => (
              <div key={room.id} className="p-4 flex justify-between items-center hover:bg-gray-50 cursor-pointer" onClick={() => navigate(`/chat/${room.id}`)}>
                <div>
                  <h3 className="font-semibold text-gray-800">{room.name}</h3>
                  <span className="text-xs text-gray-500">{room.isGroupChat ? '그룹 채팅' : '1:1 채팅'}</span>
                </div>
                <button className="text-blue-600 hover:text-blue-800 text-sm font-medium">입장 →</button>
              </div>
            ))}
            {myRooms.length === 0 && <div className="p-8 text-center text-gray-500">참여 중인 채팅방이 없습니다.</div>}
          </div>
        </div>

        {/* 공개 채팅방 섹션 */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-bold">공개 그룹 채팅방</h2>
          </div>
          <div className="flex gap-2 mb-4">
            <input 
              className="border p-2 rounded flex-grow focus:outline-none focus:ring-2 focus:ring-blue-500"
              type="text" 
              value={newRoomName} 
              onChange={(e) => setNewRoomName(e.target.value)} 
              placeholder="새 방 이름" 
            />
            <button className="bg-gray-800 hover:bg-gray-900 text-white px-4 py-2 rounded transition" onClick={createGroupRoom}>방 만들기</button>
          </div>
          
          <div className="bg-white rounded-lg shadow divide-y h-[19.5rem] overflow-y-auto">
            {allRooms.filter(r => r.isGroupChat).map(room => {
              const isJoined = myRooms.some(my => my.id === room.id);
              return (
                <div key={room.id} className="p-4 flex justify-between items-center hover:bg-gray-50">
                  <div>
                    <h3 className="font-semibold text-gray-800">{room.name}</h3>
                    <span className="text-xs text-gray-500">그룹 채팅</span>
                  </div>
                  {isJoined ? (
                    <span className="text-gray-400 text-sm font-medium px-4 py-1">참여중</span>
                  ) : (
                    <button 
                      className="border border-blue-600 text-blue-600 hover:bg-blue-50 px-4 py-1 rounded text-sm font-medium transition"
                      onClick={() => joinRoom(room.id)}>
                      참여하기
                    </button>
                  )}
                </div>
              );
            })}
            {allRooms.filter(r => r.isGroupChat).length === 0 && <div className="p-8 text-center text-gray-500">생성된 그룹 채팅방이 없습니다.</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
