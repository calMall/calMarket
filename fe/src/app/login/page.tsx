"use client";

import { login } from "@/api/User";
import CustomButton from "@/components/common/CustomBtn";
import CustomInput from "@/components/common/CustomInput";
import CustomLayout from "@/components/common/CustomLayout";
import UserStore from "@/store/user";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { IoEyeOffSharp, IoEyeSharp } from "react-icons/io5";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const userStore = UserStore();
  const router = useRouter();
  const [isHidePassword, setIsHidePassword] = useState(true);
  const onLogin = async () => {
    try {
      const data = await login(email, password);
      if (data.message === "success") {
        userStore.setUserInfo({
          nickname: data.nickname,
          cartItemCount: data.cartItemCount,
        });
        router.push("/");
      }
    } catch (e) {
      return alert("ログインに失敗しました。");
    }
  };

  return (
    <CustomLayout>
      <div className="login-box">
        <h3 className="flex jc">ログイン</h3>
        <Link className="signup-link" href={"/signup"}>
          アカウントをお持ちでない方はこちら
        </Link>
        <div className="mt-1">メール</div>
        <CustomInput
          placeholder="IDを入力してください。"
          classname="mt-05"
          text={email}
          setText={setEmail}
        />
        <div className="mt-1">パスワード</div>

        <div className="rt">
          <CustomInput
            isPassword={isHidePassword}
            placeholder="パスワードを入力してください。"
            classname="mt-05 rp-2"
            text={password}
            setText={setPassword}
            func={onLogin}
          />
          <button
            className="ab password-toggle flex ac"
            onClick={() => setIsHidePassword((prev) => !prev)}
          >
            {isHidePassword ? (
              <IoEyeSharp className="toggle-icon" />
            ) : (
              <IoEyeOffSharp className="toggle-icon" />
            )}
          </button>
        </div>
        <CustomButton func={onLogin} text="ログイン" classname="mt-2" />
      </div>
    </CustomLayout>
  );
}
