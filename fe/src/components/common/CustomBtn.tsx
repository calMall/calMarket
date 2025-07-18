import React, { Dispatch, SetStateAction } from "react";

interface props {
  classname?: string;
  text: string;
  func: Function;
}
export default function CustomButton({ classname, text, func }: props) {
  return (
    <div>
      <button
        className={"flex jc ac custom-btn " + classname}
        onClick={() => func()}
      >
        {text}
      </button>
    </div>
  );
}
