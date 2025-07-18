import React, { Dispatch, SetStateAction } from "react";

interface props {
  placeholder?: string;
  classname?: string;
  text: string;
  setText: Dispatch<SetStateAction<string>>;
}
export default function CustomInput({
  placeholder,
  classname,
  text,
  setText,
}: props) {
  return (
    <div>
      <input
        className={"custom-input " + classname}
        type="text"
        value={text}
        onChange={(e) => {
          setText(e.target.value);
        }}
        placeholder={placeholder}
      />
    </div>
  );
}
