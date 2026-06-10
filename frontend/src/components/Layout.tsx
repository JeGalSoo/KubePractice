import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { authApi } from '../api/authApi';

export default function Layout() {
  const navigate = useNavigate();
  const [user, setUser] = useState<{ name: string; email: string } | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userDataStr = localStorage.getItem('user');
    if (token && userDataStr) {
      try {
        setUser(JSON.parse(userDataStr));
      } catch (e) {
        console.error('Failed to parse user data');
      }
    }
  }, []);

  const handleLogout = () => {
    authApi.logout();
    setUser(null);
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Link to="/" className="text-xl font-bold text-indigo-600">Bulletin Board</Link>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              {user ? (
                <div className="flex items-center space-x-4">
                  <Link to="/chat" className="text-sm font-medium text-indigo-600 hover:text-indigo-800">
                    참여 중인 채팅방
                  </Link>
                  <span className="text-sm text-gray-700">{user.name} 님</span>
                  <button
                    onClick={handleLogout}
                    className="text-sm font-medium text-gray-500 hover:text-gray-700"
                  >
                    로그아웃
                  </button>
                </div>
              ) : (
                <>
                  <Link to="/login" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
                    로그인
                  </Link>
                  <Link to="/register" className="text-sm font-medium text-indigo-600 hover:text-indigo-500 border border-indigo-600 px-3 py-1 rounded">
                    회원가입
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>
      <main className="flex-1 max-w-7xl mx-auto py-6 sm:px-6 lg:px-8 w-full">
        <div className="px-4 py-6 sm:px-0">
          <Outlet />
        </div>
      </main>
      <footer className="bg-white border-t border-gray-200 mt-auto">
        <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-gray-500">
            &copy; 2026 Scalable Bulletin Board. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
