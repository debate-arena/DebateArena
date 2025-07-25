import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function OAuth2RedirectHandler() {
  const navigate = useNavigate();

  useEffect(() => {
    // 예시: 백엔드에서 JWT를 쿼리스트링 또는 해시로 전달한다고 가정
    // 예: http://localhost:3000/oauth2/redirect?token=JWT값
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    if (token) {
      localStorage.setItem("accessToken", token);
      alert("로그인 성공! JWT 저장됨");
      navigate("/");
    } else {
      alert("로그인 실패 또는 토큰 없음");
      navigate("/");
    }
  }, [navigate]);

  return <div>로그인 처리 중...</div>;
}

export default OAuth2RedirectHandler;
