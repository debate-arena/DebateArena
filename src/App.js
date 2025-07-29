import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import Login from "./Login";
import OAuth2RedirectHandler from "./OAuth2RedirectHandler";
import MatchMainTest from "./MatchMainTest";
import AuthTest from "./AuthTest";
import UserProfileTest from "./UserProfileTest";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
        <Route path="/match-main-test" element={<MatchMainTest />} />
        <Route path="/auth-test" element={<AuthTest />} />
        <Route path="/user-profile-test" element={<UserProfileTest />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
