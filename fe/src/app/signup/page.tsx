"use client";
import { checkEmail } from "@/api/User";
import CustomButton from "@/components/common/CustomBtn";
import CustomInput from "@/components/common/CustomInput";
import CustomLayout from "@/components/common/CustomLayout";
import { useState } from "react";

export default function Signup() {
  const [password, setPassword] = useState("");
  const [password2, setPassword2] = useState("");
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");

  const [isHidePassword, setIsHidePassword] = useState(true);
  const [isHidePassword2, setIsHidePassword2] = useState(true);
  const [checkedEmail, setCheckedEmail] = useState(false);

  const onCheckEmail = async () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) return alert("メールの形式ではありません。");

    try {
      const data = await checkEmail(email);
      if (data.available) {
        setCheckedEmail(true);
        alert("使用できるメールアドレスです。");
      } else return alert("すでに登録されているメールアドレスです。");
    } catch (e) {
      alert("検査に失敗しました。");
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

          <CustomButton
            disable={checkedEmail}
            text="重複検査"
            classname="check-btn "
            func={onCheckEmail}
          />
        </div>

        <div className="mt-1">
          パスワード <span className="red-color">*</span>
        </div>
        <div>
          <CustomInput
            isPassword={isHidePassword}
            placeholder="8〜20文字で入力してください。"
            classname="mt-05"
            text={password}
            setText={setPassword}
          />
        </div>

        <div className="mt-1">
          パスワード再入力 <span className="red-color">*</span>
        </div>
        <CustomInput
          isPassword={isHidePassword2}
          placeholder="パスワードを再入力してください。"
          classname="mt-05"
          text={password2}
          setText={setPassword2}
        />

        <div className="mt-1">
          ニックネーム <span className="red-color">*</span>
        </div>
        <CustomInput
          placeholder="パスワードを再入力してください。"
          classname="mt-05"
          text={nickname}
          setText={setNickname}
        />

        <div className="mt-1">生年月日</div>
        <input type="date" className="mt-05 custom-input" />

        <CustomButton text="登録" func={() => {}} classname="mt-2" />
      </div>
    </CustomLayout>
  );
}
