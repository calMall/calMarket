"use client";
import { checkEmail, signup } from "@/api/User";
import CustomButton from "@/components/common/CustomBtn";
import CustomInput from "@/components/common/CustomInput";
import CustomLayout from "@/components/common/CustomLayout";
import { useState } from "react";
import { IoEyeOffSharp, IoEyeSharp } from "react-icons/io5";
import { useRouter } from "next/navigation";

export default function Signup() {
  const router = useRouter();

  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");

  const [isHidePassword, setIsHidePassword] = useState(true);
  const [isHidePassword2, setIsHidePassword2] = useState(true);
  const [checkedEmail, setCheckedEmail] = useState(false);
  const [birth, setBirth] = useState("");
  const changeEmail = () => {
    setCheckedEmail(false);
  };

  const onCheckEmail = async () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) return alert("メールの形式ではありません。");

    try {
      const data = await checkEmail(email);
      if (data.available) {
        setCheckedEmail(true);
        return alert("使用できるメールアドレスです。");
      } else return alert("すでに登録されているメールアドレスです。");
    } catch (e) {
      alert("検査に失敗しました。");
    }
  };

  const onSignup = async () => {
    if (!checkedEmail)
      return alert("メールアドレス重複検査を完了してください。");
    if (password !== password2) return alert("パスワードが違います。");
    if (password.length < 8 || password.length > 20)
      return alert("パスワードは8~20文字にしてください。");
    if (!nickname) return alert("ニックネームを入力してください。");
    const signupData: SignupReq = {
      email: email,
      password: password,
      nickname: nickname,
      birth: birth,
    };
    try {
      const data = await signup(signupData);
      if (data.message === "success") return alert("登録されました。");
      return alert("登録に失敗しました。");
    } catch (e) {
      console.log(e);
      return alert("登録に失敗しました。");
    }
  };

  return (
    <CustomLayout>
      <div className="login-box signup-box">
        <h3 className="flex jc">会員登録</h3>

        <div className="mt-1">
          メール <span className="red-color">*</span>
        </div>

        <div className="flex ac jb wf mt-05 gap-1">
          <CustomInput
            placeholder="メールを入力してください。"
            text={email}
            setText={setEmail}
            disable={checkedEmail}
          />
          {checkedEmail ? (
            <CustomButton
              text="変更する"
              classname="check-btn "
              func={changeEmail}
            />
          ) : (
            <CustomButton
              text="重複検査"
              classname="check-btn "
              func={onCheckEmail}
            />
          )}
        </div>

        <div className="mt-1">
          パスワード <span className="red-color">*</span>
        </div>
        <div className="rt">
          <CustomInput
            isPassword={isHidePassword}
            placeholder="8〜20文字で入力してください。"
            classname="mt-05"
            text={password}
            setText={setPassword}
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

        <div className="mt-1">
          パスワード再入力 <span className="red-color">*</span>
        </div>
        <div className="rt">
          <CustomInput
            isPassword={isHidePassword2}
            placeholder="パスワードを再入力してください。"
            classname="mt-05"
            text={password2}
            setText={setPassword2}
          />
          <button
            className="ab password-toggle flex ac"
            onClick={() => setIsHidePassword2((prev) => !prev)}
          >
            {isHidePassword2 ? (
              <IoEyeSharp className="toggle-icon" />
            ) : (
              <IoEyeOffSharp className="toggle-icon" />
            )}
          </button>
        </div>

        <div className="mt-1">
          ニックネーム <span className="red-color">*</span>
        </div>
        <CustomInput
          placeholder="ニックネーム"
          classname="mt-05"
          text={nickname}
          setText={setNickname}
        />

        <div className="mt-1">生年月日</div>
        <input
          type="date"
          className="mt-05 custom-input"
          onChange={(e) => setBirth(e.target.value)}
          value={birth}
        />

        <CustomButton text="登録" func={onSignup} classname="mt-2" />
      </div>
    </CustomLayout>
  );
}
