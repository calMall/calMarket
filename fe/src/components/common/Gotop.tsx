"use client";

import { IoIosArrowUp } from "react-icons/io";

export default function GoTop() {
  const onGoTop = () => {
    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };
  return (
    <button className="go-top flex jc ac">
      <IoIosArrowUp color="white" fontSize={40} onClick={onGoTop} />
    </button>
  );
}
