import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { boardApi } from '../api/boardApi';
import type { Board, Page } from '../types';

export default function BoardListPage() {
  const [pageData, setPageData] = useState<Page<Board> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBoards(currentPage);
  }, [currentPage]);

  const fetchBoards = async (page: number) => {
    try {
      setLoading(true);
      const data = await boardApi.getBoards(page);
      setPageData(data);
    } catch (error) {
      console.error('Failed to fetch boards:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !pageData) {
    return <div className="text-center py-10">Loading...</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Posts</h1>
        <Link
          to="/boards/new"
          className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none"
        >
          Create Post
        </Link>
      </div>

      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        <ul className="divide-y divide-gray-200">
          {pageData?.content.map((board) => (
            <li key={board.id}>
              <Link to={`/boards/${board.id}`} className="block hover:bg-gray-50">
                <div className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-indigo-600 truncate">{board.title}</p>
                    <div className="ml-2 flex-shrink-0 flex">
                      <p className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                        {board.user?.username || 'Anonymous'}
                      </p>
                    </div>
                  </div>
                  <div className="mt-2 sm:flex sm:justify-between">
                    <div className="sm:flex">
                      <p className="flex items-center text-sm text-gray-500 line-clamp-1">
                        {board.content}
                      </p>
                    </div>
                    <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6">
                      <p>
                        Created at <time dateTime={board.createdAt}>{new Date(board.createdAt).toLocaleDateString()}</time>
                      </p>
                    </div>
                  </div>
                </div>
              </Link>
            </li>
          ))}
          {pageData?.content.length === 0 && (
            <li className="px-4 py-10 text-center text-gray-500">No posts available.</li>
          )}
        </ul>
      </div>

      {pageData && (
        <div className="mt-6 flex justify-center items-center gap-1">
          <button
            onClick={() => setCurrentPage(0)}
            disabled={pageData.first}
            className="px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            «
          </button>
          <button
            onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
            disabled={pageData.first}
            className="px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            ‹
          </button>
          {Array.from({ length: pageData.totalPages }, (_, i) => i)
            .filter(i => Math.abs(i - pageData.number) <= 2)
            .map(i => (
              <button
                key={i}
                onClick={() => setCurrentPage(i)}
                className={`px-3 py-2 border rounded-md shadow-sm text-sm font-medium transition-colors ${
                  i === pageData.number
                    ? 'bg-indigo-600 text-white border-indigo-600'
                    : 'border-gray-300 text-gray-700 bg-white hover:bg-gray-50'
                }`}
              >
                {i + 1}
              </button>
            ))}
          <button
            onClick={() => setCurrentPage(p => Math.min(pageData.totalPages - 1, p + 1))}
            disabled={pageData.last}
            className="px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            ›
          </button>
          <button
            onClick={() => setCurrentPage(pageData.totalPages - 1)}
            disabled={pageData.last}
            className="px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            »
          </button>
          <span className="ml-2 text-sm text-gray-500">
            {pageData.number + 1} / {pageData.totalPages} 페이지 (총 {pageData.totalElements}건)
          </span>
        </div>
      )}
    </div>
  );
}
