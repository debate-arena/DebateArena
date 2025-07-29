import React, { useState, useEffect } from "react";

const API_BASE_URL = "http://localhost:8080"; // debate-arena 서비스 포트

function AuthTest() {
  const [authStatus, setAuthStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  // 토큰 검증
  const verifyToken = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/verify`, {
        method: "GET",
        credentials: "include", // 쿠키 포함
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await response.json();
      setAuthStatus(data);
      console.log("토큰 검증 결과:", data);
      
      // ApiResponse 구조에 맞게 처리
      if (data.status === "success") {
        console.log("토큰 검증 성공:", data.data);
      } else {
        console.log("토큰 검증 실패:", data.data);
      }
    } catch (error) {
      console.error("토큰 검증 중 오류:", error);
      setAuthStatus({
        status: "error",
        data: "토큰 검증 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  // 현재 사용자 정보 조회
  const getCurrentUser = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        method: "GET",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await response.json();
      setAuthStatus(data);
      console.log("현재 사용자 정보:", data);
      
      // ApiResponse 구조에 맞게 처리
      if (data.status === "success") {
        console.log("사용자 정보 조회 성공:", data.data);
      } else {
        console.log("사용자 정보 조회 실패:", data.data);
      }
    } catch (error) {
      console.error("사용자 정보 조회 중 오류:", error);
      setAuthStatus({
        status: "error",
        data: "사용자 정보 조회 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  // 로그아웃
  const logout = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await response.json();
      setAuthStatus(data);
      console.log("로그아웃 결과:", data);
      
      // ApiResponse 구조에 맞게 처리
      if (data.status === "success") {
        console.log("로그아웃 성공:", data.data);
      } else {
        console.log("로그아웃 실패:", data.data);
      }
    } catch (error) {
      console.error("로그아웃 중 오류:", error);
      setAuthStatus({
        status: "error",
        data: "로그아웃 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>JWT 인증 테스트</h2>
      
      <div style={{ marginBottom: "20px" }}>
        <button 
          onClick={verifyToken} 
          disabled={loading}
          style={{ marginRight: "10px" }}
        >
          토큰 검증
        </button>
        
        <button 
          onClick={getCurrentUser} 
          disabled={loading}
          style={{ marginRight: "10px" }}
        >
          현재 사용자 정보
        </button>
        
        <button 
          onClick={logout} 
          disabled={loading}
          style={{ backgroundColor: "#ff4444", color: "white" }}
        >
          로그아웃
        </button>
      </div>

      {loading && <div>로딩 중...</div>}

      {authStatus && (
        <div style={{ 
          marginTop: "20px", 
          padding: "15px", 
          border: "1px solid #ccc", 
          borderRadius: "5px",
          backgroundColor: authStatus.status === "success" ? "#e8f5e8" : "#ffe8e8"
        }}>
          <h3>응답 결과:</h3>
          <div>
            <strong>Status:</strong> {authStatus.status}
          </div>
          <div>
            <strong>Data:</strong>
          </div>
          <pre>{JSON.stringify(authStatus.data, null, 2)}</pre>
        </div>
      )}

      <div style={{ marginTop: "20px", fontSize: "14px", color: "#666" }}>
        <h4>사용 방법:</h4>
        <ol>
          <li>먼저 "구글로 로그인" 버튼을 눌러 로그인합니다.</li>
          <li>"토큰 검증" 버튼을 눌러 JWT 토큰이 유효한지 확인합니다.</li>
          <li>"현재 사용자 정보" 버튼으로 로그인된 사용자 정보를 확인합니다.</li>
          <li>"로그아웃" 버튼으로 쿠키의 토큰을 만료시킵니다.</li>
        </ol>
      </div>
    </div>
  );
}

export default AuthTest; 