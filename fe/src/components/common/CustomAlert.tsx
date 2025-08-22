import { FaCheck } from "react-icons/fa";

interface props {
  text: string;
  status: "loading" | "success";
}
export default function CustomAlert({ text, status }: props) {
  return (
    <div className="custom-alert flex ac">
      <div className="flex ac jc">
        {status === "loading" ? (
          <img src={"/spinner.gif"} alt="" />
        ) : (
          <div className="rt">
            <svg className="circle-draw flex ac jc" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="45"></circle>
            </svg>
            <FaCheck className="ab check" />
          </div>
        )}
      </div>
      <div>{text}</div>
    </div>
  );
}
