import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { boardApi } from '../api/boardApi';

export default function BoardFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditMode = Boolean(id);
  
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEditMode) {
      const fetchBoard = async () => {
        try {
          const data = await boardApi.getBoard(Number(id));
          setTitle(data.title);
          setContent(data.content);
        } catch (error) {
          console.error('Failed to fetch board:', error);
          navigate('/');
        }
      };
      fetchBoard();
    }
  }, [id, isEditMode, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isEditMode) {
        await boardApi.updateBoard(Number(id), { title, content });
        alert('Updated successfully');
        navigate(`/boards/${id}`);
      } else {
        const newBoard = await boardApi.createBoard({ title, content });
        alert('Created successfully');
        navigate(`/boards/${newBoard.id}`);
      }
    } catch (error: any) {
      console.error('Failed to save post:', error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        alert('Authentication failed. You need valid credentials to save a post.');
      } else {
        alert('An error occurred while saving the post.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white shadow sm:rounded-lg max-w-2xl mx-auto">
      <div className="px-4 py-5 sm:p-6">
        <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
          {isEditMode ? 'Edit Post' : 'Create New Post'}
        </h3>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="title" className="block text-sm font-medium text-gray-700">
              Title
            </label>
            <div className="mt-1">
              <input
                type="text"
                name="title"
                id="title"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md p-2 border"
              />
            </div>
          </div>

          <div>
            <label htmlFor="content" className="block text-sm font-medium text-gray-700">
              Content
            </label>
            <div className="mt-1">
              <textarea
                id="content"
                name="content"
                rows={10}
                required
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border border-gray-300 rounded-md p-2"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-400"
            >
              {loading ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
