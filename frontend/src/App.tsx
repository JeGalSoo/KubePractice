import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import BoardListPage from './pages/BoardListPage';
import BoardDetailPage from './pages/BoardDetailPage';
import BoardFormPage from './pages/BoardFormPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ChatListPage from './pages/chat/ChatListPage';
import ChatRoomPage from './pages/chat/ChatRoomPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<BoardListPage />} />
          <Route path="boards/new" element={<BoardFormPage />} />
          <Route path="boards/:id" element={<BoardDetailPage />} />
          <Route path="boards/:id/edit" element={<BoardFormPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="chat" element={<ChatListPage />} />
          <Route path="chat/:roomId" element={<ChatRoomPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
