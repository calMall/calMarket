import React, { Dispatch, SetStateAction } from "react";

interface props {
  classname?: string;
  text: string;
  func: Function;
  disable?: boolean;
}
export default function CustomButton({
  classname,
  text,
  func,
  disable,
}: props) {
  return (
    <div>
      <button
        disabled={disable}
        className={"flex jc ac custom-btn " + classname}
        onClick={() => func()}
      >
        {text}
      </button>
    </div>
  );
}
