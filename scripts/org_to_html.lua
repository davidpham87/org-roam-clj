function Link(el)
   if not el.target:find("^http") then
      el.target = "./" .. string.gsub(el.target, "%.org", ".md")
   end
   return el
end
