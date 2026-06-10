import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { boardApi } from '../api/boardApi';
import type { Board } from '../types';

export default function BoardDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [board, setBoard] = useState<Board | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) {
      fetchBoard(Number(id));
    }
  }, [id]);

  const fetchBoard = async (boardId: number) => {
    try {
      setLoading(true);
      const data = await boardApi.getBoard(boardId);
      setBoard(data);
    } catch (error) {
      console.error('Failed to fetch board:', error);
      alert('Post not found');
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this post?')) return;
    
    try {
      await boardApi.deleteBoard(Number(id));
      alert('Deleted successfully');
      navigate('/');
    } catch (error) {
      console.error('Failed to delete board:', error);
      alert('Failed to delete. Ensure you are authorized.');
    }
  };

  if (loading) {
    return <div className="text-center py-10">Loading...</div>;
  }

  if (!board) {
    return null;
  }

  return (
    <div className="bg-white shadow overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6 flex justify-between items-center">
        <div>
          <h3 className="text-lg leading-6 font-medium text-gray-900">{board.title}</h3>
          <p className="mt-1 max-w-2xl text-sm text-gray-500">
            By {board.user?.username || 'Anonymous'} | {new Date(board.createdAt).toLocaleString()}
          </p>
        </div>
        <div className="flex space-x-3">
          <Link
            to={`/boards/${board.id}/edit`}
            className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none"
          >
            Edit
          </Link>
          <button
            onClick={handleDelete}
            className="inline-flex items-center px-3 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none"
          >
            Delete
          </button>
        </div>
      </div>
      <div className="border-t border-gray-200 px-4 py-5 sm:px-6">
        <div className="whitespace-pre-wrap text-gray-700">
          {board.content}
        </div>
      </div>
    </div>
  );
}
