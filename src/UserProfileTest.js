import React, { useState, useEffect } from "react";

const API_BASE_URL = "http://localhost:8080"; // debate-arena 서비스 포트

function UserProfileTest() {
  const [nickname, setNickname] = useState("");
  const [checkResult, setCheckResult] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);

  // 닉네임 중복 확인
  const checkNickname = async () => {
    if (!nickname.trim()) {
      alert("닉네임을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/user/nickname/check?nickname=${encodeURIComponent(nickname)}`, {
        method: "GET",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await response.json();
      setCheckResult(data);
      console.log("닉네임 중복 확인 결과:", data);
    } catch (error) {
      console.error("닉네임 중복 확인 중 오류:", error);
      setCheckResult({
        status: "error",
        data: "닉네임 중복 확인 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  // 닉네임 업데이트
  const updateNickname = async () => {
    if (!nickname.trim()) {
      alert("닉네임을 입력해주세요.");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/user/nickname`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ nickname: nickname }),
      });

      const data = await response.json();
      setCheckResult(data);
      console.log("닉네임 업데이트 결과:", data);
      
      if (data.status === "success") {
        // 업데이트 성공 시 프로필 다시 조회
        getProfile();
      }
    } catch (error) {
      console.error("닉네임 업데이트 중 오류:", error);
      setCheckResult({
        status: "error",
        data: "닉네임 업데이트 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  // 프로필 조회
  const getProfile = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/user/profile`, {
        method: "GET",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      });

      const data = await response.json();
      setProfile(data);
      console.log("프로필 조회 결과:", data);
    } catch (error) {
      console.error("프로필 조회 중 오류:", error);
      setProfile({
        status: "error",
        data: "프로필 조회 중 오류가 발생했습니다."
      });
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트 마운트 시 프로필 조회
  useEffect(() => {
    getProfile();
  }, []);

  return (
    <div style={{ padding: "20px" }}>
      <h2>사용자 프로필 테스트</h2>
      
      {/* 현재 프로필 정보 */}
      <div style={{ marginBottom: "30px" }}>
        <h3>현재 프로필 정보</h3>
        <button 
          onClick={getProfile} 
          disabled={loading}
          style={{ marginBottom: "10px" }}
        >
          프로필 새로고침
        </button>
        
        {profile && (
          <div style={{ 
            padding: "15px", 
            border: "1px solid #ccc", 
            borderRadius: "5px",
            backgroundColor: profile.status === "success" ? "#e8f5e8" : "#ffe8e8"
          }}>
            <div>
              <strong>Status:</strong> {profile.status}
            </div>
            <div>
              <strong>Data:</strong>
            </div>
            <pre>{JSON.stringify(profile.data, null, 2)}</pre>
          </div>
        )}
      </div>

      {/* 닉네임 중복 확인 및 업데이트 */}
      <div style={{ marginBottom: "30px" }}>
        <h3>닉네임 관리</h3>
        <div style={{ marginBottom: "15px" }}>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="닉네임을 입력하세요 (2-20자)"
            style={{ 
              padding: "8px", 
              marginRight: "10px", 
              width: "200px" 
            }}
            maxLength={20}
          />
          <button 
            onClick={checkNickname} 
            disabled={loading || !nickname.trim()}
            style={{ marginRight: "10px" }}
          >
            중복 확인
          </button>
          <button 
            onClick={updateNickname} 
            disabled={loading || !nickname.trim()}
            style={{ backgroundColor: "#4CAF50", color: "white" }}
          >
            닉네임 업데이트
          </button>
        </div>

        {loading && <div>로딩 중...</div>}

        {checkResult && (
          <div style={{ 
            padding: "15px", 
            border: "1px solid #ccc", 
            borderRadius: "5px",
            backgroundColor: checkResult.status === "success" ? "#e8f5e8" : "#ffe8e8"
          }}>
            <h4>결과:</h4>
            <div>
              <strong>Status:</strong> {checkResult.status}
            </div>
            <div>
              <strong>Data:</strong>
            </div>
            <pre>{JSON.stringify(checkResult.data, null, 2)}</pre>
          </div>
        )}
      </div>

      <div style={{ marginTop: "20px", fontSize: "14px", color: "#666" }}>
        <h4>사용 방법:</h4>
        <ol>
          <li>먼저 "구글로 로그인" 버튼을 눌러 로그인합니다.</li>
          <li>"프로필 새로고침" 버튼으로 현재 프로필 정보를 확인합니다.</li>
          <li>닉네임 입력 필드에 원하는 닉네임을 입력합니다.</li>
          <li>"중복 확인" 버튼으로 닉네임 사용 가능 여부를 확인합니다.</li>
          <li>"닉네임 업데이트" 버튼으로 닉네임을 변경합니다.</li>
        </ol>
      </div>
    </div>
  );
}

export default UserProfileTest; 