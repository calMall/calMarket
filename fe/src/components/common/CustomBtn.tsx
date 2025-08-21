import React from "react";

interface props {
  classname?: string;
  text: string;
  func: Function;
  disable?: boolean;
  icon?: React.ReactNode;
}
export default function CustomButton({
  classname,
  text,
  func,
  disable,
  icon,
}: props) {
  return (
    <div>
      <button
        disabled={disable}
        className={"flex jc ac custom-btn " + classname}
        onClick={() => func()}
      >
        {icon ? icon : text}
      </button>
    </div>
  );
}
