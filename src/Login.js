import React from "react";
import { useNavigate } from "react-router-dom";

const GOOGLE_AUTH_URL = "http://localhost:8080/oauth2/authorization/google";

function Login() {
  const navigate = useNavigate();
  return (
    <div>
      <h2>구글 소셜 로그인 테스트</h2>
      <a href={GOOGLE_AUTH_URL}>
        <button>구글로 로그인</button>
      </a>
      <br /><br />
      <button onClick={() => navigate("/match-main-test")}>매칭 메인 테스트로 이동</button>
      <br /><br />
      <button onClick={() => navigate("/auth-test")}>JWT 인증 테스트로 이동</button>
      <br /><br />
      <button onClick={() => navigate("/user-profile-test")}>사용자 프로필 테스트로 이동</button>
    </div>
  );
}

export default Login;