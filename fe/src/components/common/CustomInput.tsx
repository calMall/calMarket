import React, { Dispatch, SetStateAction } from "react";

interface props {
  placeholder?: string;
  classname?: string;
  disable?: boolean;
  isPassword?: boolean;
  text: string;
  func?: Function;
  setText: Dispatch<SetStateAction<string>>;
}
export default function CustomInput({
  placeholder,
  classname,
  text,
  isPassword,
  disable,
  setText,
  func,
}: props) {
  return (
    <input
      disabled={disable}
      className={"custom-input " + classname}
      type={isPassword ? "password" : "text"}
      value={text}
      onChange={(e) => {
        setText(e.target.value);
      }}
      {...(func ? { onKeyDown: (e) => e.key === "Enter" && func() } : {})}
      placeholder={placeholder}
    />
  );
}
